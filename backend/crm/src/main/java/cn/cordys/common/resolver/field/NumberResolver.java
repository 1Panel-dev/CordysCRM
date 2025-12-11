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
			BigDecimal bd = new BigDecimal(text);
			boolean illegal = checkIllegalDecimal(bd);
			if (illegal) {
				throw new FormulaParseException("数值超出范围, 整数不超过9位, 小数不超过4位;");
			}
			return bd;
        } catch (NumberFormatException e) {
            throw new FormulaParseException("无法解析数值类型: " + text);
        }
    }

	/**
	 * 是否非法的数值范围 (整数不超过9位, 小数不超过4位)
	 * @param value 数值
	 * @return 是否非法
	 */
	private boolean checkIllegalDecimal(BigDecimal value) {
		if (value == null) {
			return false;
		}
		// 小数位
		int scale = value.scale();
		int precision = value.precision();
		// 整数位
		int integerDigits = precision - scale;

		return integerDigits > 9 || scale > 4;
	}
}
