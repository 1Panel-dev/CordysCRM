package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.InputNumberField;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * @author jianxing
 */
public class NumberResolver extends AbstractModuleFieldResolver<InputNumberField> {

    public static final String PERCENT_FORMAT = "percent";
    public static final String PERCENT_SUFFIX = "%";

    @Override
    public void validate(InputNumberField numberField, Object value) {
        validateRequired(numberField, value);

        if (value != null && !(value instanceof Number)) {
            throwValidateException(numberField.getName());
        }
    }

    @Override
    public Object parse2Value(InputNumberField numberField, String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public Object text2Value(InputNumberField field, String text) {
        if (StringUtils.equals(field.getNumberFormat(), PERCENT_FORMAT)) {
            return text.replace(PERCENT_SUFFIX, StringUtils.EMPTY);
        }
        return text;
    }
}
