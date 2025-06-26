package io.cordys.crm.system.service;

import io.cordys.common.constants.TopicConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.redis.MessagePublisher;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import io.cordys.file.engine.DefaultRepositoryDir;
import io.cordys.file.engine.FileRequest;
import io.cordys.file.engine.StorageType;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
public class ExportTaskCenterService {

    @Resource
    private BaseMapper<ExportTask> exportTaskMapper;
    @Resource
    private MessagePublisher messagePublisher;
    @Resource
    private FileCommonService fileCommonService;

    /**
     * 查询导出任务列表
     *
     * @param request 请求参数
     * @return 导出任务列表
     */
    public List<ExportTask> list(ExportTaskCenterQueryRequest request, String userId) {
        LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(1);
        LambdaQueryWrapper<ExportTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ExportTask::getFileName, request.getKeyword())
                .eq(ExportTask::getCreateUser, userId)
                .gtT(ExportTask::getCreateTime, oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .orderByDesc(ExportTask::getCreateTime);
        if (StringUtils.isNotEmpty(request.getExportType())) {
            queryWrapper.eq(ExportTask::getResourceType, request.getExportType());
        }
        if (StringUtils.isNotEmpty(request.getExportStatus())) {
            queryWrapper.eq(ExportTask::getStatus, request.getExportStatus());
        }
        return exportTaskMapper.selectListByLambda(queryWrapper);
    }

    /**
     * 取消导出任务
     *
     * @param taskId 任务ID
     */
    public void cancel(String taskId) {
        ExportTask exportTask = exportTaskMapper.selectByPrimaryKey(taskId);
        if (exportTask == null) {
            throw new GenericException(Translator.get("task_not_found"));
        }
        if (StringUtils.equalsIgnoreCase(exportTask.getStatus(), ExportConstants.ExportStatus.STOP.name())) {
            throw new GenericException(Translator.get("task_already_stopped"));
        }
        exportTask.setStatus(ExportConstants.ExportStatus.STOP.name());
        exportTaskMapper.updateById(exportTask);
        //停止任务
        messagePublisher.publish(TopicConstants.DOWNLOAD_TOPIC, taskId);
    }

    /**
     * 下载任务文件
     *
     * @param taskId 任务ID
     */
    public ResponseEntity<org.springframework.core.io.Resource> download(String taskId) {
        ExportTask exportTask = exportTaskMapper.selectByPrimaryKey(taskId);
        if (exportTask == null) {
            throw new GenericException(Translator.get("task_not_found"));
        }
        String filePath = getFilePath(exportTask);
        try {
            FileRequest request = new FileRequest(filePath, StorageType.LOCAL.name(), exportTask.getFileName() + ".xlsx");
            return fileCommonService.download(request);
        } catch (Exception e) {
            LogUtils.error("下载导出任务文件失败，任务ID: " + taskId, e);
            throw new RuntimeException(e);
        }
    }

    private String getFilePath(ExportTask exportTask) {
        return DefaultRepositoryDir.getExportDir() + "/" + exportTask.getFileId();
    }

    public void clean() {
        LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(1);
        long epochMilli = oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        LambdaQueryWrapper<ExportTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ltT(ExportTask::getCreateTime, epochMilli).orderByDesc(ExportTask::getCreateTime);
        List<ExportTask> exportTasks = exportTaskMapper.selectListByLambda(queryWrapper);
        exportTaskMapper.deleteByLambda(queryWrapper);

        exportTasks.forEach(exportTask -> {
            try {
                //删除文件
                FileRequest request = new FileRequest(getFilePath(exportTask), StorageType.LOCAL.name(), exportTask.getFileName() + ".xlsx");
                fileCommonService.deleteFolder(request);
            } catch (GenericException e) {
                LogUtils.error("定时任务删除导出任务文件失败，任务ID: " + exportTask.getId(), e);
                throw new RuntimeException(e);
            }
        });

    }
}
