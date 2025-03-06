package io.cordys.common.resolver.field;

import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.base.BaseField;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-03-06  16:07
 */
public class DefaultModuleFieldResolver extends AbstractModuleFieldResolver {
    @Override
    public void validate(BaseField customField, Object value) {
        // 校验必填
        validateRequired(customField, value);
    }

    @Override
    public String parse2String(BaseField selectField, Object value) {
        if (value instanceof List) {
            return JSON.toJSONString(value);
        }
        return value.toString();
    }
}
