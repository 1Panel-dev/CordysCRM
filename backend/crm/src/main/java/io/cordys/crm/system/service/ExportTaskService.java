package io.cordys.crm.system.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("ExportTaskService")
@Transactional(rollbackFor = Exception.class)
public class ExportTaskService {

    @Resource
    private BaseMapper<ExportTask> exportTaskMapper;

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
}
