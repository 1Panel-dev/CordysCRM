package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.constants.FollowUpPlanStatusType;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpPlanField;
import cn.cordys.crm.follow.domain.FollowUpPlanFieldBlob;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.domain.FollowUpRecordField;
import cn.cordys.crm.follow.domain.FollowUpRecordFieldBlob;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.idev.excel.FastExcelFactory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 跟进记录和跟进计划共用的自定义字段导入处理。 */
@Service
@Slf4j
public class FollowUpImportService {

    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseService baseService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpRecordField> followUpRecordFieldMapper;
    @Resource
    private BaseMapper<FollowUpRecordFieldBlob> followUpRecordFieldBlobMapper;
    @Resource
    private FollowUpRecordFieldService followUpRecordFieldService;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private BaseMapper<FollowUpPlanField> followUpPlanFieldMapper;
    @Resource
    private BaseMapper<FollowUpPlanFieldBlob> followUpPlanFieldBlobMapper;
    @Resource
    private FollowUpPlanFieldService followUpPlanFieldService;

    public void downloadRecordTemplate(HttpServletResponse response, String orgId) {
        downloadTemplate(response, FormKey.FOLLOW_RECORD, orgId, "follow_record.import_tpl.name");
    }

    public void downloadPlanTemplate(HttpServletResponse response, String orgId) {
        downloadTemplate(response, FormKey.FOLLOW_PLAN, orgId, "follow_plan.import_tpl.name");
    }

    private void downloadTemplate(HttpServletResponse response, FormKey formKey, String orgId, String nameKey) {
        new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
                moduleFormService.getCustomImportHeadsNoRef(formKey.getKey(), orgId), Translator.get(nameKey),
                Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(formKey.getKey(), orgId)),
                new CustomHeadColWidthStyleStrategy());
    }

    public ImportResponse preCheckRecord(MultipartFile file, String importType, String orgId) {
        return preCheck(file, importType, orgId, FormKey.FOLLOW_RECORD, "follow_up_record", "follow_up_record_field");
    }

    public ImportResponse preCheckPlan(MultipartFile file, String importType, String orgId) {
        return preCheck(file, importType, orgId, FormKey.FOLLOW_PLAN, "follow_up_plan", "follow_up_plan_field");
    }

    private ImportResponse preCheck(MultipartFile file, String importType, String orgId, FormKey formKey,
            String tableName, String fieldTableName) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(formKey.getKey(), orgId);
            CustomFieldCheckEventListener listener = new CustomFieldCheckEventListener(fields, tableName, fieldTableName,
                    orgId, importType);
            FastExcelFactory.read(file.getInputStream(), listener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(listener.getErrList()).successCount(listener.getSuccess())
                    .failCount(listener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("{} import pre-check error", formKey.getKey(), e);
            throw new GenericException(e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse importRecord(MultipartFile file, ImportRequest request, String orgId, String userId) {
        return importRecordOrPlan(file, request, orgId, userId, true);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse importPlan(MultipartFile file, ImportRequest request, String orgId, String userId) {
        return importRecordOrPlan(file, request, orgId, userId, false);
    }

    private ImportResponse importRecordOrPlan(MultipartFile file, ImportRequest request, String orgId, String userId,
            boolean record) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        try {
            if (record) {
                return importRecords(file, request, orgId, userId);
            }
            return importPlans(file, request, orgId, userId);
        } catch (Exception e) {
            log.error("follow-up import error", e);
            throw new GenericException("导入异常，请检查文件数据！" + e.getMessage());
        }
    }

    private ImportResponse importRecords(MultipartFile file, ImportRequest request, String orgId, String userId)
            throws Exception {
        List<BaseField> fields = moduleFormService.getAllFields(FormKey.FOLLOW_RECORD.getKey(), orgId);
        ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
        CustomImportAfterDoConsumer<FollowUpRecord, BaseResourceSubField> afterDo = (records, values, blobs) ->
                saveBatch(records, followUpRecordMapper, followUpRecordFieldService, FollowUpRecord::getOrganizationId,
                        importType, orgId, userId, LogModule.FOLLOW_UP_RECORD,
                        record -> record.setCommentCount(0L),
                        () -> {
                            followUpRecordFieldMapper.batchInsert(values.stream()
                                    .map(value -> BeanUtils.copyBean(new FollowUpRecordField(), value)).toList());
                            followUpRecordFieldBlobMapper.batchInsert(blobs.stream()
                                    .map(blob -> BeanUtils.copyBean(new FollowUpRecordFieldBlob(), blob)).toList());
                        });
        return read(file, fields, FollowUpRecord.class, orgId, userId, "follow_up_record_field",
                "follow_up_record_field_blob", afterDo, request.getImportType(), FormKey.FOLLOW_RECORD);
    }

    private ImportResponse importPlans(MultipartFile file, ImportRequest request, String orgId, String userId)
            throws Exception {
        List<BaseField> fields = moduleFormService.getAllFields(FormKey.FOLLOW_PLAN.getKey(), orgId);
        ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
        CustomImportAfterDoConsumer<FollowUpPlan, BaseResourceSubField> afterDo = (plans, values, blobs) ->
                saveBatch(plans, followUpPlanMapper, followUpPlanFieldService, FollowUpPlan::getOrganizationId,
                        importType, orgId, userId, LogModule.FOLLOW_UP_PLAN,
                        plan -> {
                            plan.setCommentCount(0L);
                            plan.setConverted(false);
                            plan.setStatus(FollowUpPlanStatusType.PREPARED.name());
                        },
                        () -> {
                            followUpPlanFieldMapper.batchInsert(values.stream()
                                    .map(value -> BeanUtils.copyBean(new FollowUpPlanField(), value)).toList());
                            followUpPlanFieldBlobMapper.batchInsert(blobs.stream()
                                    .map(blob -> BeanUtils.copyBean(new FollowUpPlanFieldBlob(), blob)).toList());
                        });
        return read(file, fields, FollowUpPlan.class, orgId, userId, "follow_up_plan_field", "follow_up_plan_field_blob",
                afterDo, request.getImportType(), FormKey.FOLLOW_PLAN);
    }

    private <T extends BaseModel> void saveBatch(List<T> resources, BaseMapper<T> mapper,
            BaseResourceFieldService<?, ?> fieldService, Function<T, String> organizationId, ImportType importType,
            String orgId, String userId, String module, Consumer<T> initialize, Runnable saveFields) {
        boolean add = importType == ImportType.ADD;
        List<String> ids = resources.stream().map(BaseModel::getId).distinct().toList();
        Map<String, T> originalResources = add ? Map.of() : getResources(mapper, ids, organizationId, orgId);
        Map<String, List<BaseModuleFieldValue>> originalFields = add ? Map.of()
                : fieldService.getResourceFieldMap(new ArrayList<>(originalResources.keySet()), true);
        if (importType == ImportType.UPDATE) {
            fieldService.deleteByResourceIds(ids);
        }

        List<String> savedIds = new ArrayList<>();
        for (T resource : resources) {
            int affected;
            if (add) {
                initialize.accept(resource);
                affected = mapper.insert(resource);
            } else {
                affected = mapper.update(resource);
            }
            if (affected > 0 || originalResources.containsKey(resource.getId())) {
                savedIds.add(resource.getId());
            }
        }
        saveFields.run();
        writeImportLogs(savedIds.stream().distinct().toList(), mapper, fieldService, organizationId,
                orgId, userId, module, add, originalResources, originalFields);
    }

    private <T extends BaseModel> void writeImportLogs(List<String> ids, BaseMapper<T> mapper,
            BaseResourceFieldService<?, ?> fieldService, Function<T, String> organizationId, String orgId,
            String userId, String module, boolean add, Map<String, T> originalResources,
            Map<String, List<BaseModuleFieldValue>> originalFields) {
        if (ids.isEmpty()) {
            return;
        }
        boolean plan = LogModule.FOLLOW_UP_PLAN.equals(module);
        if (add) {
            String name = Translator.get(plan ? "message.follow_up_plan_add" : "message.follow_up_record_add");
            logService.batchAdd(ids.stream()
                    .map(id -> new LogDTO(orgId, id, userId, LogType.ADD, module, name))
                    .toList());
            return;
        }

        String name = Translator.get(plan ? "update_follow_up_plan" : "update_follow_up_record");
        Map<String, T> updatedResources = getResources(mapper, ids, organizationId, orgId);
        Map<String, List<BaseModuleFieldValue>> updatedFields =
                fieldService.getResourceFieldMap(new ArrayList<>(updatedResources.keySet()), true);
        List<LogDTO> logs = new ArrayList<>();
        for (String id : ids) {
            T original = originalResources.get(id);
            T updated = updatedResources.get(id);
            if (original == null || updated == null) {
                continue;
            }
            try {
                baseService.handleUpdateLog(original, updated, originalFields.get(id), updatedFields.get(id), id, name);
                LogContextInfo context = OperationLogContext.getContext();
                if (context != null) {
                    LogDTO logDTO = new LogDTO(orgId, id, userId, LogType.UPDATE, module, name);
                    logDTO.setOriginalValue(context.getOriginalValue());
                    logDTO.setModifiedValue(context.getModifiedValue());
                    logs.add(logDTO);
                }
            } finally {
                OperationLogContext.clear();
            }
        }
        logService.batchAdd(logs);
    }

    private <T extends BaseModel> Map<String, T> getResources(BaseMapper<T> mapper, List<String> ids,
            Function<T, String> organizationId, String orgId) {
        return mapper.selectByIds(ids).stream()
                .filter(resource -> orgId.equals(organizationId.apply(resource)))
                .collect(Collectors.toMap(BaseModel::getId, Function.identity()));
    }

    private <T> ImportResponse read(MultipartFile file, List<BaseField> fields, Class<T> type, String orgId,
            String userId, String fieldTable, String blobTable, CustomImportAfterDoConsumer<T, BaseResourceSubField> afterDo,
            String importType, FormKey formKey) throws Exception {
        CustomFieldImportEventListener<T> listener = new CustomFieldImportEventListener<>(fields, type, orgId, userId,
                fieldTable, blobTable, afterDo, 2000, null, null, importType, formKey.getKey());
        FastExcelFactory.read(file.getInputStream(), listener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
        return ImportResponse.builder().errorMessages(listener.getErrList()).successCount(listener.getSuccessCount())
                .failCount(listener.getErrList().size()).build();
    }
}
