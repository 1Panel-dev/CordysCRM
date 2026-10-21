package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.base.BaseField;

import java.util.List;

/**
 * @author jianxing
 */
public class DefaultModuleFieldResolver extends AbstractModuleFieldResolver {
    @Override
    public void validate(BaseField customField, Object value) {
        // 校验必填
        validateRequired(customField, value);
    }

    @Override
    public String convertToString(BaseField selectField, Object value) {
        if (value instanceof List) {
            return getJsonString(value);
        }
        return getStringValue(value);
    }
}
