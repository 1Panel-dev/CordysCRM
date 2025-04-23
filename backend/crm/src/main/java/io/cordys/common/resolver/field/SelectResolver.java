package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.SelectField;

/**
 * @author jianxing
 */
public class SelectResolver extends AbstractModuleFieldResolver<SelectField> {

    @Override
    public void validate(SelectField selectField, Object value) {

        // 校验必填
        validateRequired(selectField, value);

        // 校验值类型
        validateString(selectField.getName(), value);

        // 校验选项正确
        validateOptions(selectField.getName(), value, selectField.getOptions());
    }

    @Override
    public String parse2String(SelectField selectField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(SelectField selectField, String value) {
        return value;
    }
}
