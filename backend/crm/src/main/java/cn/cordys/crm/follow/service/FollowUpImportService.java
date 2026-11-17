package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
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

import java.util.List;

/** 跟进记录和跟进计划共用的自定义字段导入处理。 */
@Service
@Slf4j
public class FollowUpImportService {

    @Resource
    private ModuleFormService moduleFormService;
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
        CustomImportAfterDoConsumer<FollowUpRecord, BaseResourceSubField> afterDo = (records, values, blobs) -> {
            if (EnumUtils.valueOf(ImportType.class, request.getImportType()) == ImportType.UPDATE) {
                followUpRecordFieldService.deleteByResourceIds(records.stream().map(FollowUpRecord::getId).toList());
            }
            records.forEach(record -> {
                if (EnumUtils.valueOf(ImportType.class, request.getImportType()) == ImportType.ADD) {
                    record.setCommentCount(0L);
                    followUpRecordMapper.insert(record);
                } else {
                    followUpRecordMapper.update(record);
                }
            });
            followUpRecordFieldMapper.batchInsert(values.stream()
                    .map(value -> BeanUtils.copyBean(new FollowUpRecordField(), value)).toList());
            followUpRecordFieldBlobMapper.batchInsert(blobs.stream()
                    .map(blob -> BeanUtils.copyBean(new FollowUpRecordFieldBlob(), blob)).toList());
        };
        return read(file, fields, FollowUpRecord.class, orgId, userId, "follow_up_record_field",
                "follow_up_record_field_blob", afterDo, request.getImportType(), FormKey.FOLLOW_RECORD);
    }

    private ImportResponse importPlans(MultipartFile file, ImportRequest request, String orgId, String userId)
            throws Exception {
        List<BaseField> fields = moduleFormService.getAllFields(FormKey.FOLLOW_PLAN.getKey(), orgId);
        CustomImportAfterDoConsumer<FollowUpPlan, BaseResourceSubField> afterDo = (plans, values, blobs) -> {
            if (EnumUtils.valueOf(ImportType.class, request.getImportType()) == ImportType.UPDATE) {
                followUpPlanFieldService.deleteByResourceIds(plans.stream().map(FollowUpPlan::getId).toList());
            }
            plans.forEach(plan -> {
                if (EnumUtils.valueOf(ImportType.class, request.getImportType()) == ImportType.ADD) {
                    plan.setCommentCount(0L);
                    plan.setConverted(false);
                    plan.setStatus(FollowUpPlanStatusType.PREPARED.name());
                    followUpPlanMapper.insert(plan);
                } else {
                    followUpPlanMapper.update(plan);
                }
            });
            followUpPlanFieldMapper.batchInsert(values.stream()
                    .map(value -> BeanUtils.copyBean(new FollowUpPlanField(), value)).toList());
            followUpPlanFieldBlobMapper.batchInsert(blobs.stream()
                    .map(blob -> BeanUtils.copyBean(new FollowUpPlanFieldBlob(), blob)).toList());
        };
        return read(file, fields, FollowUpPlan.class, orgId, userId, "follow_up_plan_field", "follow_up_plan_field_blob",
                afterDo, request.getImportType(), FormKey.FOLLOW_PLAN);
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
