package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.formula.FormulaParseException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author jianxing
 */
public class FormulaResolver extends AbstractModuleFieldResolver<FormulaField> {

    @Override
    public void validate(FormulaField numberField, Object value) {
        validateRequired(numberField, value);

        if (value != null && !(value instanceof Number) && !(value instanceof String)) {
            throwValidateException(numberField.getName());
        }
    }

    @Override
    public Object convertToValue(FormulaField numberField, String value) {
        if (value == null || !Strings.CI.equals(numberField.getFormulaResultFormat(), "number")) return value;
        // 读持久化原值不应用显示精度，也不注入千分位。
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return value; // 数值模式允许公式返回合法文本。
        }
    }

    @Override
    public Object transformToValue(FormulaField numberField, String value) {
        if (value == null) {
            return null;
        }
        try {
            if (Strings.CI.equals(numberField.getFormulaResultFormat(), "number")) {
                BigDecimal actualDecimal = new BigDecimal(value).stripTrailingZeros();
                actualDecimal = actualDecimal.setScale(BooleanUtils.isTrue(numberField.getDecimalPlaces())
                        ? numberField.getPrecision() : 0, RoundingMode.DOWN);
                String formatActualVal;
                if (BooleanUtils.isTrue(numberField.getShowThousandsSeparator())) {
                    formatActualVal = InputNumberField.formatThousands(actualDecimal);
                } else {
                    formatActualVal = actualDecimal.toPlainString();
                }
                return formatActualVal;
            }
            return value;
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public Object textToValue(FormulaField field, String text) {
        try {
            if (BooleanUtils.isTrue(field.getShowThousandsSeparator())) {
                text = text.replace(",", StringUtils.EMPTY).replace("，", StringUtils.EMPTY);
            }
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
