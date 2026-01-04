package cn.cordys.crm.system.service;

import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.redis.MessagePublisher;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import cn.cordys.crm.system.mapper.ExtExportTaskMapper;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
@Slf4j
public class ExportTaskCenterService {

    @Resource
    private BaseMapper<ExportTask> exportTaskMapper;
    @Resource
    private MessagePublisher messagePublisher;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private ExtExportTaskMapper extExportTaskMapper;

    /**
     * 查询导出任务列表
     *
     * @param request 请求参数
     *
     * @return 导出任务列表
     */
    public List<ExportTask> list(ExportTaskCenterQueryRequest request, String userId) {
        LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(1);
        return extExportTaskMapper.selectExportTaskList(request, oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), userId);
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
        if (Strings.CI.equals(exportTask.getStatus(), ExportConstants.ExportStatus.SUCCESS.name())) {
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
            log.error("下载导出任务文件失败，任务ID: " + taskId, e);
            throw new RuntimeException(e);
        }
    }

    private String getFilePath(ExportTask exportTask) {
        return File.separator + DefaultRepositoryDir.getExportDir(exportTask.getOrganizationId()) + File.separator + exportTask.getFileId();
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
                fileCommonService.deleteFolder(request, false);
            } catch (GenericException e) {
                log.error("定时任务删除导出任务文件失败，任务ID: " + exportTask.getId(), e);
                throw new RuntimeException(e);
            }
        });

    }
}
