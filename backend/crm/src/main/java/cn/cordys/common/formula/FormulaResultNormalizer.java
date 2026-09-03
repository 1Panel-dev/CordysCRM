package cn.cordys.common.formula;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 对齐前端 normalizeFormulaResult/keepDecimal 的结果归一化。
 */
public class FormulaResultNormalizer {

    private static final MathContext FORMULA_PRECISION = new MathContext(15, RoundingMode.HALF_UP);

    public Object normalize(Object result, int decimalPlaces, ExpectedType expectedType) {
        if (result == null) {
            return "";
        }
        if (expectedType == ExpectedType.STRING) {
            return javaScriptString(result);
        }
        if (result instanceof Number number) {
            return keepDecimal(number.doubleValue(), decimalPlaces);
        }
        if (result instanceof Boolean bool) {
            return bool ? "TRUE" : "FALSE";
        }
        return String.valueOf(result);
    }

    public String javaScriptString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean bool) {
            return Boolean.toString(bool);
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isNaN(doubleValue)) {
                return "NaN";
            }
            if (doubleValue == Math.rint(doubleValue)) {
                return Long.toString((long) doubleValue);
            }
            return Double.toString(doubleValue);
        }
        return String.valueOf(value);
    }

    private double keepDecimal(double value, int digits) {
        if (!Double.isFinite(value)) {
            return value;
        }
        double normalized = BigDecimal.valueOf(value).round(FORMULA_PRECISION).doubleValue();
        String text = Double.toString(normalized);
        int dot = text.indexOf('.');
        if (dot < 0 || text.contains("E") || text.contains("e")) {
            return normalized;
        }
        int end = Math.min(text.length(), dot + 1 + Math.max(digits, 0));
        String truncated = text.substring(0, end);
        if (truncated.endsWith(".")) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return Double.parseDouble(truncated);
    }

    public enum ExpectedType {
        STRING,
        UNSPECIFIED
    }
}
