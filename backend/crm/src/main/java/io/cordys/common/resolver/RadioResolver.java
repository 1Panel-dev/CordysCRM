package io.cordys.common.resolver;


import io.cordys.common.resolver.field.AbstractModuleFieldResolver;
import io.cordys.crm.system.dto.field.RadioField;

/**
 * @author jianxing
 */
public class RadioResolver extends AbstractModuleFieldResolver<RadioField> {

    @Override
    public void validate(RadioField radioField, Object value) {

        // 校验必填
        validateRequired(radioField, value);

        // 校验值类型
        validateString(radioField.getName(), value);

        // 校验选项正确
        validateOptions(radioField.getName(), value, radioField.getOptions());
    }

    @Override
    public String parse2String(RadioField radioField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(RadioField radioField, String value) {
        return value;
    }
}
