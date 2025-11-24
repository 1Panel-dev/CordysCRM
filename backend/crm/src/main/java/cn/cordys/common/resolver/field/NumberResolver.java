package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.InputNumberField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.formula.FormulaParseException;

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
    public Object convertToValue(InputNumberField numberField, String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public Object transformToValue(InputNumberField numberField, String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public Object textToValue(InputNumberField field, String text) {
        if (Strings.CS.equals(field.getNumberFormat(), PERCENT_FORMAT)) {
            text = text.replace(PERCENT_SUFFIX, StringUtils.EMPTY);
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new FormulaParseException("无法解析数值类型: " + text);
        }
    }
}
