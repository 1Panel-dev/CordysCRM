package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
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
 * 在 Jakarta Bean Validation 之前补全写请求中的公式字段。
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

    public void complete(String formKey, Object request) {
        complete(formKey, request, true);
    }

    /**
     * 根据请求类型补全公式字段。更新时使用已提交的真实流水号值，不生成新增占位符。
     */
    public void complete(String formKey, Object request, boolean createMode) {
        completeAuthoritative(formKey, request, createMode, OrganizationContext.getOrganizationId());
    }

    /** 动态表单在服务层权限检查后，使用完整记录与实时公式重新计算。 */
    public void completeAuthoritative(String formKey, Object request, boolean createMode, String orgId) {
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
        java.util.function.BiConsumer<String, Object> writer = (businessKey, value) -> {
            if (requestValues.isWritableProperty(businessKey)) {
                requestValues.setPropertyValue(businessKey, value);
            }
        };
        formulaCompletionService.complete(fields, moduleFieldMap, createMode, readableValue(requestValues), writer);
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
