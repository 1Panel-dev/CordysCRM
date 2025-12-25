package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.service.BaseChartService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.dto.request.ClueExportRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.dto.request.ClueChartAnalysisDbRequest;
import cn.cordys.crm.customer.dto.request.PoolClueChartAnalysisRequest;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
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
    private ExtClueMapper extClueMapper;
    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private BaseChartService baseChartService;


    public String exportCrossPage(ClueExportRequest request, String userId, String orgId, DeptDataPermissionDTO dataPermission, Locale locale) {
        checkFileName(request.getFileName());
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE_POOL.toString(), request.getFileName());

        runExport(orgId, userId, LogModule.CLUE_POOL_INDEX, locale, exportTask, request.getFileName(),
                () -> exportData(fileId, exportTask, userId, request, orgId, dataPermission));

        return exportTask.getId();
    }

    private void exportData(String fileId, ExportTask exportTask, String userId, ClueExportRequest request, String orgId, DeptDataPermissionDTO dataPermission) throws InterruptedException {
        //表头信息
        List<List<String>> headList = request.getHeadList().stream()
                .map(head -> Collections.singletonList(head.getTitle()))
                .toList();

        batchHandleData(fileId,
                headList,
                exportTask,
                request.getFileName(),
                request,
                t -> getExportData(request, userId, orgId, dataPermission, exportTask.getId()));
    }

    @Override
    public String exportSelect(ExportSelectRequest request, String userId, String orgId, Locale locale) {
        checkFileName(request.getFileName());
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE_POOL.toString(), request.getFileName());

        runExport(orgId, userId, LogModule.CLUE_POOL_INDEX, locale, exportTask, request.getFileName(),
                () -> exportData(fileId, exportTask, userId, request, orgId));

        return exportTask.getId();
    }

    private void exportData(String fileId, ExportTask exportTask,
                            String userId, ExportSelectRequest request,
                            String orgId) {
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
    }

    public List<ChartResult> chart(PoolClueChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = clueService.getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        ClueChartAnalysisDbRequest clueChartAnalysisDbRequest = BeanUtils.copyBean(new ClueChartAnalysisDbRequest(), chartAnalysisDbRequest);
        clueChartAnalysisDbRequest.setPoolId(request.getPoolId());
        List<ChartResult> chartResults = extClueMapper.chart(clueChartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }
}
