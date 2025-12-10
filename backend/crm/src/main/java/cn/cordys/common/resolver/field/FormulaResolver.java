package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.FormulaField;
import org.apache.poi.ss.formula.FormulaParseException;

import java.math.BigDecimal;

/**
 * @author jianxing
 */
public class FormulaResolver extends AbstractModuleFieldResolver<FormulaField> {

    @Override
    public void validate(FormulaField numberField, Object value) {
        validateRequired(numberField, value);

        if (value != null && !(value instanceof Number)) {
            throwValidateException(numberField.getName());
        }
    }

    @Override
    public Object convertToValue(FormulaField numberField, String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public Object transformToValue(FormulaField numberField, String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public Object textToValue(FormulaField field, String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new FormulaParseException("无法解析数值类型: " + text);
        }
    }
}
