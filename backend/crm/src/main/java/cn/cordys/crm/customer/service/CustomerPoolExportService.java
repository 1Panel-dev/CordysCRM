package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerPoolExportService extends CustomerExportService {

    @Resource
    private CustomerExportService customerExportService;
    @Resource
    private ExportTaskService exportTaskService;


    /**
     * 跨页导出公海池数据
     *
     * @param userId
     * @param request
     * @param orgId
     * @param deptDataPermission
     * @param locale
     * @return
     */
    public String exportCrossPage(String userId, CustomerExportRequest request, String orgId, DeptDataPermissionDTO deptDataPermission, Locale locale) {
        checkFileName(request.getFileName());
        //用户导出数量 限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());
        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CUSTOMER_POOL.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                customerExportService.exportCustomerData(exportTask, userId, request, orgId, deptDataPermission, locale);
            } catch (InterruptedException e) {
                LogUtils.error("任务停止中断", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            } catch (Exception e) {
                //更新任务
                LogUtils.error("任务停止中断", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CUSTOMER_POOL, request.getFileName());
            }
        });
        return exportTask.getId();
    }

    public String exportCrossSelect(String userId, ExportSelectRequest request, String orgId, Locale locale) {
        checkFileName(request.getFileName());
        // 用户导出数量限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());
        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CUSTOMER_POOL.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                this.exportSelectData(exportTask, userId, request, orgId, locale);
            } catch (Exception e) {
                LogUtils.error("导出公海客户异常", e);
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CUSTOMER_POOL, request.getFileName());
            }
        });
        return exportTask.getId();
    }
}
