package cn.cordys.common.resolver.field;


import cn.cordys.crm.system.dto.field.RadioField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

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
                .filter(option -> Strings.CI.equals(option.getValue(), value))
                .map(OptionProp::getLabel)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public Object text2Value(RadioField field, String text) {
        return field.getOptions().stream()
                .filter(option -> Strings.CI.equals(option.getLabel(), text))
                .map(OptionProp::getValue)
                .findFirst()
                .orElse(text);
    }
}
