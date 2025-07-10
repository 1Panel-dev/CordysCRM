package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.RadioField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.lang3.StringUtils;

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
    public Object parse2Value(RadioField selectField, String value) {
        return super.parse2Value(selectField, value);
    }

    @Override
    public Object trans2Value(RadioField radioField, String value) {
        return radioField.getOptions().stream()
                .filter(option -> StringUtils.equalsIgnoreCase(option.getValue(), value))
                .map(OptionProp::getLabel)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }
}
