package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueExportRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(rollbackFor = Exception.class)
public class CluePoolExportService extends ClueExportService {

    @Resource
    private ClueService clueService;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExportTaskService exportTaskService;

    public String exportCrossPage(ClueExportRequest request, String userId, String orgId, DeptDataPermissionDTO dataPermission, Locale locale) {
        checkFileName(request.getFileName());
        //用户导出数量 限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE_POOL.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                LocaleContextHolder.setLocale(locale);
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
                //表头信息
                List<List<String>> headList = request.getHeadList().stream()
                        .map(head -> Collections.singletonList(head.getTitle()))
                        .toList();

                //分批查询数据并写入文件
                batchHandleData(fileId,
                        headList,
                        exportTask,
                        request.getFileName(),
                        request,
                        t -> getExportData(request, userId, orgId, dataPermission, exportTask.getId()));

                //更新状态
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);

            } catch (InterruptedException e) {
                LogUtils.error("任务停止中断", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            } catch (Exception e) {
                //更新任务
                LogUtils.error("任务失败", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CLUE_POOL_INDEX, request.getFileName());
            }
        });
        return exportTask.getId();
    }

    @Override
    public String exportSelect(ExportSelectRequest request, String userId, String orgId, Locale locale) {
        checkFileName(request.getFileName());
        // 用户导出数量限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE_POOL.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                LocaleContextHolder.setLocale(locale);
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
                //表头信息
                List<List<String>> headList = request.getHeadList().stream()
                        .map(head -> Collections.singletonList(head.getTitle()))
                        .toList();

                // 准备导出文件
                File file = prepareExportFile(fileId, request.getFileName(), exportTask.getOrganizationId());
                try (ExcelWriter writer = EasyExcel.write(file)
                        .head(headList)
                        .excelType(ExcelTypeEnum.XLSX)
                        .build()) {
                    WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

                    SubListUtils.dealForSubList(request.getIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (subIds) -> {
                        List<List<Object>> data = new ArrayList<>();
                        try {
                            data = getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId());
                        } catch (InterruptedException e) {
                            LogUtils.error("任务停止中断", e);
                            exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
                        }
                        writer.write(data, sheet);
                    });
                }

                //更新导出任务状态
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);
            } catch (Exception e) {
                LogUtils.error("导出线索池异常", e);
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CLUE_POOL_INDEX, request.getFileName());
            }
        });
        return exportTask.getId();
    }
}
