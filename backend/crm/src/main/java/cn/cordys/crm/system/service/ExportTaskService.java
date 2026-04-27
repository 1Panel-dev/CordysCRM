package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.mapper.ExtExportTaskMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("ExportTaskService")
@Transactional(rollbackFor = Exception.class)
public class ExportTaskService {

    @Resource
    private BaseMapper<ExportTask> exportTaskMapper;
    @Resource
    private ExtExportTaskMapper extExportTaskMapper;

    public ExportTask saveTask(String orgId, String fileId, String userId, String resourceType, String fileName) {
        ExportTask exportTask = new ExportTask();
        exportTask.setId(IDGenerator.nextStr());
        exportTask.setResourceType(resourceType);
        exportTask.setCreateUser(userId);
        exportTask.setCreateTime(System.currentTimeMillis());
        exportTask.setStatus(ExportConstants.ExportStatus.PREPARED.toString());
        exportTask.setUpdateUser(userId);
        exportTask.setFileName(fileName);
        exportTask.setUpdateTime(System.currentTimeMillis());
        exportTask.setOrganizationId(orgId);
        exportTask.setFileId(fileId);
        exportTaskMapper.insert(exportTask);
        return exportTask;
    }

    public void update(String taskId, String status, String userId) {
        ExportTask exportTask = new ExportTask();
        exportTask.setId(taskId);
        exportTask.setStatus(status);
        exportTask.setUpdateTime(System.currentTimeMillis());
        exportTask.setUpdateUser(userId);
        exportTaskMapper.updateById(exportTask);
    }

    public void checkUserTaskLimit(String userId, String resourceType) {
        // 检查是否存在相同资源类型的待处理任务，防止重复点击导致大量相同任务积压
        int count = extExportTaskMapper.checkPreparedUniqueness(userId, resourceType);
        if (count > 0) {
            throw new GenericException(Translator.get("user_export_task_prepared_limit"));
        }
        int userTaskCount = extExportTaskMapper.getExportTaskCount(userId, ExportConstants.ExportStatus.PREPARED.name());
        if (userTaskCount >= 10) {
            throw new GenericException(Translator.get("user_export_task_limit"));
        }
    }
}
