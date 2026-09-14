package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 显式调用的公式请求适配器，仅由 MCP 专用预计算入口使用，不参与 CRM 普通写入生命周期。
 *
 * <p>公式始终取服务端当前组织的实时表单，而不是信任客户端随请求传入的表单快照。
 * 请求 DTO 只需沿用既有业务属性和 {@code moduleFields}，不增加任何给 AI 生成的参数。</p>
 */
@Service
public class FormulaRequestCompletionService {

    private final ModuleFormService moduleFormService;
    private final FormulaCompletionService formulaCompletionService;

    public FormulaRequestCompletionService(
            ModuleFormService moduleFormService,
            FormulaCompletionService formulaCompletionService
    ) {
        this.moduleFormService = moduleFormService;
        this.formulaCompletionService = formulaCompletionService;
    }

    /** MCP 专用预计算：只修改请求副本，不读写业务记录，不挂接 CRM 保存生命周期。 */
    public Map<String, Object> prepare(String formKey, Map<String, Object> values) {
        Map<String, Object> result = cn.cordys.common.util.JSON.parseToMap(
                cn.cordys.common.util.JSON.toJSONString(values));
        Map<String, BaseModuleFieldValue> moduleFields = new LinkedHashMap<>();
        if (result.get("moduleFields") instanceof List<?> list) {
            for (Object item : list) {
                BaseModuleFieldValue value = cn.cordys.common.util.JSON.parseObject(
                        cn.cordys.common.util.JSON.toJSONString(item), BaseModuleFieldValue.class);
                if (value == null || value.getFieldId() == null) {
                    throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED);
                }
                moduleFields.put(value.getFieldId(), value);
            }
        }
        List<BaseField> fields = moduleFormService.getAllFields(formKey, OrganizationContext.getOrganizationId());
        if (fields == null || fields.isEmpty()) throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED);
        try {
            formulaCompletionService.complete(fields, moduleFields, result.get("id") == null,
                    result::get, result::put);
        } catch (FormulaEvaluationException e) {
            GenericException error = new GenericException(CrmHttpResultCode.VALIDATE_FAILED,
                    Translator.get("formula.calculation.failed"));
            error.initCause(e);
            throw error;
        }
        result.put("moduleFields", new ArrayList<>(moduleFields.values()));
        return result;
    }

    public void complete(String formKey, Object request) {
        complete(formKey, request, true);
    }

    /**
     * 根据请求类型补全公式字段。更新时使用已提交的真实流水号值，不生成新增占位符。
     */
    public void complete(String formKey, Object request, boolean createMode) {
        completeAuthoritative(formKey, request, createMode, OrganizationContext.getOrganizationId());
    }

    /** 显式计算适配方法；不由业务保存服务自动调用。 */
    public void completeAuthoritative(String formKey, Object request, boolean createMode, String orgId) {
        completeWithBaseline(formKey, request, createMode, orgId, Map.of());
    }

    public void completeUpdate(String formKey, Object request, Map<String, Object> baseline) {
        completeWithBaseline(formKey, request, false, OrganizationContext.getOrganizationId(), baseline);
    }

    private void completeWithBaseline(String formKey, Object request, boolean createMode,
            String orgId, Map<String, Object> baseline) {
        if (request == null) {
            return;
        }
        List<BaseField> fields = moduleFormService.getAllFields(
                formKey, orgId);
        if (fields == null || fields.isEmpty()) {
            return;
        }

        BeanWrapper requestValues = new BeanWrapperImpl(request);
        Map<String, BaseModuleFieldValue> moduleFieldMap = moduleFieldMap(requestValues);
        Map<String, BaseModuleFieldValue> oldFields = new LinkedHashMap<>();
        if (baseline.get("moduleFields") instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof BaseModuleFieldValue field) oldFields.put(field.getFieldId(), field);
            }
        }
        Map<String, BaseModuleFieldValue> merged = new LinkedHashMap<>(oldFields);
        merged.putAll(moduleFieldMap);
        for (BaseField field : fields) {
            String key = field.getBusinessKey();
            if (!createMode && field.isSerialNumber() && oldFields.containsKey(field.getId())) {
                merged.put(field.getId(), oldFields.get(field.getId()));
            }
            if (key != null && !key.isBlank() && requestValues.isWritableProperty(key)
                    && baseline.containsKey(key)
                    && (field.isSerialNumber() || requestValues.getPropertyValue(key) == null)) {
                requestValues.setPropertyValue(key, baseline.get(key));
            }
        }
        moduleFieldMap = merged;
        java.util.function.BiConsumer<String, Object> writer = (businessKey, value) -> {
            if (requestValues.isWritableProperty(businessKey)) {
                requestValues.setPropertyValue(businessKey, value);
            }
        };
        try {
            formulaCompletionService.complete(fields, moduleFieldMap, createMode, key -> {
                Object value = readableValue(requestValues).apply(key);
                return value == null ? baseline.get(key) : value;
            }, writer);
        } catch (FormulaEvaluationException e) {
            GenericException error = new GenericException(CrmHttpResultCode.VALIDATE_FAILED,
                    Translator.get("formula.calculation.failed"));
            error.initCause(e);
            throw error;
        }
        writeModuleFields(requestValues, moduleFieldMap);
    }

    private Function<String, Object> readableValue(BeanWrapper requestValues) {
        return businessKey -> requestValues.isReadableProperty(businessKey)
                ? requestValues.getPropertyValue(businessKey)
                : null;
    }

    private Map<String, BaseModuleFieldValue> moduleFieldMap(BeanWrapper requestValues) {
        Map<String, BaseModuleFieldValue> result = new LinkedHashMap<>();
        if (!requestValues.isReadableProperty("moduleFields")) {
            return result;
        }
        Object value = requestValues.getPropertyValue("moduleFields");
        if (!(value instanceof List<?> values)) {
            return result;
        }
        for (Object item : values) {
            if (item instanceof BaseModuleFieldValue fieldValue && fieldValue.getFieldId() != null) {
                result.put(fieldValue.getFieldId(), fieldValue);
            }
        }
        return result;
    }

    private void writeModuleFields(
            BeanWrapper requestValues,
            Map<String, BaseModuleFieldValue> moduleFieldMap
    ) {
        if (!requestValues.isWritableProperty("moduleFields")) {
            return;
        }
        requestValues.setPropertyValue("moduleFields", new ArrayList<>(moduleFieldMap.values()));
    }
}
