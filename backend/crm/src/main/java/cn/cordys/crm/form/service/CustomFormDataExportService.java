package cn.cordys.crm.form.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataExportRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomFormDataExportService extends BaseExportService {

    @Resource
    private CustomFormDataService customFormDataService;
    @Resource
    private ExtCustomFormDataMapper extCustomFormDataMapper;
    @Resource
    private ExportTaskService exportTaskService;

    public String exportAll(CustomFormDataExportRequest request, String userId, String orgId, Locale locale) {
        checkFileName(request.getFileName());
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.CUSTOM_FORM_DATA.name());

        // 权限检查：获取数据权限范围（导出时跳过表单启用状态检查）
        CustomFormRoleKey dataScope = customFormDataService.getDataScope(request.getCustomFormId(), userId, false);
        boolean manageOwn = dataScope == CustomFormRoleKey.MANAGE_OWN;

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId,
                ExportConstants.ExportType.CUSTOM_FORM_DATA.toString(), request.getFileName());

        runExport(orgId, userId, LogModule.CUSTOM_FORM_DATA, locale, exportTask, request.getFileName(),
                () -> exportData(fileId, exportTask, userId, request, orgId, manageOwn));

        return exportTask.getId();
    }

    public String exportSelect(ExportSelectRequest request, String userId, String orgId, Locale locale) {
        checkFileName(request.getFileName());
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.CUSTOM_FORM_DATA.name());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId,
                ExportConstants.ExportType.CUSTOM_FORM_DATA.toString(), request.getFileName());

        runExport(orgId, userId, LogModule.CUSTOM_FORM_DATA, locale, exportTask, request.getFileName(),
                () -> exportSelectData(exportTask, request, orgId, fileId, userId));

        return exportTask.getId();
    }

    private void exportData(String fileId, ExportTask exportTask, String userId,
                            CustomFormDataExportRequest request, String orgId,
                            boolean manageOwn) throws InterruptedException {
        List<List<String>> headList = request.getHeadList().stream()
                .map(head -> Collections.singletonList(head.getTitle()))
                .toList();

        batchHandleData(fileId, headList, exportTask, request.getFileName(), request,
                t -> getExportData(t, orgId, userId, manageOwn, exportTask.getId()));
    }

    private void exportSelectData(ExportTask exportTask, ExportSelectRequest request,
                                  String orgId, String fileId, String userId) {
        List<List<String>> headList = request.getHeadList().stream()
                .map(head -> Collections.singletonList(head.getTitle()))
                .toList();

        File file = prepareExportFile(fileId, request.getFileName(), exportTask.getOrganizationId());
        try (ExcelWriter writer = EasyExcel.write(file)
                .head(headList)
                .excelType(ExcelTypeEnum.XLSX)
                .build()) {
            WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

            SubListUtils.dealForSubList(request.getIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (subIds) -> {
                List<List<Object>> data = new ArrayList<>();
                try {
                    data = getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId(), userId);
                } catch (InterruptedException e) {
                    log.error("任务停止中断", e);
                    exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), exportTask.getCreateUser());
                }
                writer.write(data, sheet);
            });
        }

        exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), exportTask.getCreateUser());
    }

    private List<List<Object>> getExportData(CustomFormDataExportRequest request, String orgId,
                                              String userId, boolean manageOwn, String taskId)
            throws InterruptedException {
        int limit = request.getPageSize();
        int offset = (request.getCurrent() - 1) * request.getPageSize();
        List<CustomFormDataListResponse> rawList = extCustomFormDataMapper.listForExport(request, orgId, userId, manageOwn, limit, offset);
        return buildExportResult(request.getHeadList(), rawList, request.getCustomFormId(), orgId, taskId);
    }

    private List<List<Object>> getExportDataBySelect(List<ExportHeadDTO> headList, List<String> ids,
                                                     String orgId, String taskId, String userId) throws InterruptedException {
        List<CustomFormDataListResponse> rawList = extCustomFormDataMapper.getListByIds(ids);
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }
        String formId = rawList.getFirst().getCustomFormId();

        // 权限过滤：根据数据权限过滤选中数据（导出时跳过表单启用状态检查）
        CustomFormRoleKey dataScope = customFormDataService.getDataScope(formId, userId, false);
        if (dataScope == CustomFormRoleKey.MANAGE_OWN) {
            rawList = rawList.stream()
                    .filter(item -> Strings.CI.equals(item.getOwner(), userId))
                    .collect(Collectors.toList());
        }
        if (rawList.isEmpty()) {
            return Collections.emptyList();
        }

        return buildExportResult(headList, rawList, formId, orgId, taskId);
    }

    private List<List<Object>> buildExportResult(List<ExportHeadDTO> headList,
                                                  List<CustomFormDataListResponse> rawList,
                                                  String formId, String orgId, String taskId) throws InterruptedException {
        // 构建含数据源显示字段的数据
        CustomFormDataFieldService.setFormKey(formId);
        List<CustomFormDataListResponse> dataList;
        try {
            dataList = customFormDataService.buildList(rawList, formId, orgId);
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }

        // 字段配置
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(formId, orgId);

        List<List<Object>> result = new ArrayList<>(dataList.size());
        for (CustomFormDataListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            result.add(buildData(headList, response, fieldConfigMap));
        }

        return result;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, CustomFormDataListResponse data,
                                   Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        // 系统字段
        LinkedHashMap<String, Object> systemFieldMap = getSystemFieldMap(data);
        // 自定义字段
        AtomicReference<Map<String, Object>> moduleFieldMap = new AtomicReference<>(new LinkedHashMap<>());
        Optional.ofNullable(data.getModuleFields()).ifPresent(moduleFields ->
                moduleFieldMap.set(moduleFields.stream()
                        .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue))));
        // 处理数据转换
        transModuleFieldValue(headList, systemFieldMap, moduleFieldMap.get(), dataList, fieldConfigMap);
        return dataList;
    }

    private LinkedHashMap<String, Object> getSystemFieldMap(CustomFormDataListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }
}
