package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.SelectMultipleField;

import java.util.List;

/**
 * @author jianxing
 */
public class SelectMultipleResolver extends AbstractModuleFieldResolver<SelectMultipleField> {

    @Override
    public void validate(SelectMultipleField selectField, Object value) {
        // 校验必填
        validateRequired(selectField, value);

        // 校验值类型
        validateArray(selectField.getName(), value);

        // 校验选项正确
        for (String item : (List<String>) value) {
            validateOptions(selectField.getName(), item, selectField.getOptions());
        }
    }

    @Override
    public String parse2String(SelectMultipleField selectField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(SelectMultipleField selectField, String value) {
        return parse2Array(value);
    }
}
