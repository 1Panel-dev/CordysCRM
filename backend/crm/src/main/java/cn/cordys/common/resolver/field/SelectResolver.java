package cn.cordys.common.resolver.field;


import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

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
    public String convertToString(SelectField selectField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object convertToValue(SelectField selectField, String value) {
        return super.convertToValue(selectField, value);
    }

    @Override
    public Object transformToValue(SelectField selectField, String value) {
        return selectField.getOptions().stream()
                .filter(option -> Strings.CI.equals(option.getValue(), value))
                .findFirst()
                .map(OptionProp::getLabel)
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public Object textToValue(SelectField field, String text) {
        return field.getOptions().stream()
                .filter(option -> Strings.CI.equals(option.getLabel(), text))
                .findFirst()
                .map(OptionProp::getValue)
                .orElse(text);
    }
}
