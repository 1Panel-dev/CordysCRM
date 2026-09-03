package cn.cordys.common.formula;

import cn.cordys.common.formula.FormulaFieldMetadata.ValueType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 与前端 formula-runtime 等效的封闭 IR 求值器。
 *
 * <p>不执行 source 文本、不使用 SpEL/脚本引擎，只接受前端保存的白名单 IR 节点与函数。</p>
 */
@Component
public class FormulaEngine {

    private static final double DAY_MILLIS = 24D * 60 * 60 * 1000;
    private static final LocalDateTime EXCEL_EPOCH = LocalDate.of(1899, 12, 30).atStartOfDay();
    private static final MathContext FORMULA_PRECISION = new MathContext(15, RoundingMode.HALF_UP);

    private final FormulaDefinitionParser definitionParser;

    public FormulaEngine() {
        this(new FormulaDefinitionParser());
    }

    FormulaEngine(FormulaDefinitionParser definitionParser) {
        this.definitionParser = definitionParser;
    }

    public Object evaluate(String formulaJson, FormulaEvaluationContext context) {
        return evaluate(definitionParser.parse(formulaJson), context);
    }

    public Object evaluate(FormulaNode node, FormulaEvaluationContext context) {
        return switch (node) {
            case FormulaNode.Literal literal -> evaluateLiteral(literal);
            case FormulaNode.Field field -> evaluateField(field, context);
            case FormulaNode.Binary binary -> evaluateBinary(binary, context);
            case FormulaNode.Compare compare -> evaluateCompare(compare, context);
            case FormulaNode.Function function -> evaluateFunction(function, context);
            case FormulaNode.Invalid invalid -> {
                context.warn("INVALID_IR", invalid.reason());
                yield null;
            }
        };
    }

    public Set<String> referencedFieldIds(String formulaJson) {
        Set<String> result = new LinkedHashSet<>();
        collectReferencedFields(definitionParser.parse(formulaJson), result);
        return result;
    }

    private void collectReferencedFields(FormulaNode node, Set<String> result) {
        switch (node) {
            case FormulaNode.Field field -> result.add(field.fieldId());
            case FormulaNode.Binary binary -> {
                collectReferencedFields(binary.left(), result);
                collectReferencedFields(binary.right(), result);
            }
            case FormulaNode.Compare compare -> {
                collectReferencedFields(compare.left(), result);
                collectReferencedFields(compare.right(), result);
            }
            case FormulaNode.Function function -> function.args()
                    .forEach(arg -> collectReferencedFields(arg, result));
            default -> {
                // 字面量和无效节点不引用字段。
            }
        }
    }

    private Object evaluateLiteral(FormulaNode.Literal literal) {
        return switch (literal.valueType().toLowerCase(Locale.ROOT)) {
            case "number" -> finiteNumberOrZero(literal.value());
            case "string" -> literal.value() == null ? "" : literal.value().toString();
            case "boolean" -> literal.value() instanceof Boolean value
                    ? value : Boolean.parseBoolean(String.valueOf(literal.value()));
            default -> literal.value();
        };
    }

    private Object evaluateField(FormulaNode.Field field, FormulaEvaluationContext context) {
        String fieldId = field.fieldId();
        if (fieldId.contains(".")) {
            String[] path = fieldId.split("\\.", 2);
            Object tableValue = context.value(path[0]);
            if (!(tableValue instanceof List<?> rows)) {
                return List.of();
            }
            List<Object> result = new ArrayList<>(rows.size());
            for (Object rowValue : rows) {
                Object rawValue = rowValue instanceof java.util.Map<?, ?> row ? row.get(path[1]) : null;
                Object displayValue = context.resolveDisplayValue(fieldId, rawValue);
                result.add(resolveFieldValue(displayValue, fieldId, path[1], context));
            }
            return result;
        }

        Object rawValue = context.scalarValue(fieldId);
        Object displayValue = context.resolveDisplayValue(fieldId, rawValue);
        return resolveFieldValue(displayValue, fieldId, fieldId, context);
    }

    private Object resolveFieldValue(
            Object rawValue,
            String metadataKey,
            String fallbackMetadataKey,
            FormulaEvaluationContext context
    ) {
        FormulaFieldMetadata metadata = context.metadata(metadataKey);
        if (metadata == null) {
            metadata = context.metadata(fallbackMetadataKey);
        }

        if (metadata != null
                && "SERIAL_NUMBER".equals(metadata.fieldType())
                && !metadata.referenceField()
                && context.createMode()) {
            return "${" + metadata.name() + "}";
        }

        boolean empty = rawValue == null
                || rawValue instanceof String text && text.trim().isEmpty();
        ValueType valueType = metadata == null ? ValueType.UNKNOWN : metadata.valueType();
        if (empty) {
            return valueType == ValueType.DATE || valueType == ValueType.NUMBER ? null : "";
        }

        if (rawValue instanceof List<?>) {
            return rawValue;
        }

        if (valueType == ValueType.DATE) {
            return parseDateSerial(rawValue, context.evaluationNow().atZone(ZoneId.systemDefault()).getZone());
        }
        if (valueType == ValueType.STRING) {
            return String.valueOf(rawValue);
        }
        if (valueType == ValueType.BOOLEAN) {
            return toJavaScriptBoolean(rawValue);
        }

        double number = parseFieldNumber(rawValue);
        if (Double.isNaN(number)) {
            return 0D;
        }
        if (metadata != null && metadata.numberType() == FormulaFieldMetadata.NumberType.PERCENT) {
            return number / 100D;
        }
        return number;
    }

    private Object evaluateBinary(FormulaNode.Binary binary, FormulaEvaluationContext context) {
        Object left = evaluate(binary.left(), context);
        Object right = evaluate(binary.right(), context);
        return switch (binary.operator()) {
            case "+" -> javaScriptAdd(left, right);
            case "-" -> numericBinary(left, right, context, "SUBTRACT", (l, r) -> l - r);
            case "*" -> numericBinary(left, right, context, "MULTIPLY", (l, r) -> l * r);
            case "/" -> {
                double divisor = toNumber(right);
                if (divisor == 0D) {
                    yield 0D;
                }
                yield numericBinary(left, right, context, "DIVIDE", (l, r) -> l / r);
            }
            default -> {
                context.warn("UNKNOWN_OPERATOR", binary.operator());
                yield null;
            }
        };
    }

    private Object evaluateCompare(FormulaNode.Compare compare, FormulaEvaluationContext context) {
        Object left = evaluate(compare.left(), context);
        Object right = evaluate(compare.right(), context);
        if (isDateField(compare.left(), context) || isDateField(compare.right(), context)) {
            left = normalizeDateSerialToSecond(left);
            right = normalizeDateSerialToSecond(right);
        }
        return excelCompare(left, right, compare.operator());
    }

    private boolean isDateField(FormulaNode node, FormulaEvaluationContext context) {
        if (!(node instanceof FormulaNode.Field field)) {
            return false;
        }
        FormulaFieldMetadata metadata = context.metadata(field.fieldId());
        return metadata != null && metadata.valueType() == ValueType.DATE;
    }

    private Object normalizeDateSerialToSecond(Object value) {
        if (!(value instanceof Number number)) {
            return value;
        }
        double serial = number.doubleValue();
        return Math.floor(serial * DAY_MILLIS / 1000D) * 1000D / DAY_MILLIS;
    }

    private Object evaluateFunction(FormulaNode.Function function, FormulaEvaluationContext context) {
        List<Supplier<Object>> lazyArgs = function.args().stream()
                .map(arg -> (Supplier<Object>) () -> evaluate(arg, context))
                .toList();
        return switch (function.name()) {
            case "SUM" -> sum(evaluateAll(lazyArgs));
            case "DAYS" -> days(evaluateAll(lazyArgs));
            case "CONCATENATE" -> concatenate(evaluateAll(lazyArgs));
            case "TEXT" -> text(evaluateAll(lazyArgs));
            case "IF" -> ifFunction(lazyArgs);
            case "IFS" -> ifs(lazyArgs);
            case "AND" -> and(lazyArgs);
            case "TODAY" -> today(context.evaluationNow());
            case "NOW" -> localDateTimeToExcelSerial(context.evaluationNow());
            default -> {
                context.warn("UNKNOWN_FUNCTION", function.name());
                yield null;
            }
        };
    }

    private List<Object> evaluateAll(List<Supplier<Object>> args) {
        return args.stream().map(Supplier::get).toList();
    }

    private double sum(List<?> values) {
        double result = 0D;
        for (Object value : values) {
            if (value instanceof List<?> nested) {
                result += sum(nested);
            } else if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                result += number.doubleValue();
            }
        }
        return normalizeFormulaNumber(result);
    }

    private double days(List<Object> args) {
        if (args.size() < 2 || !(args.get(0) instanceof Number end) || !(args.get(1) instanceof Number start)) {
            return 0D;
        }
        if (!Double.isFinite(end.doubleValue()) || !Double.isFinite(start.doubleValue())) {
            return 0D;
        }
        return Math.floor(end.doubleValue()) - Math.floor(start.doubleValue());
    }

    private String concatenate(List<?> args) {
        StringBuilder result = new StringBuilder();
        appendConcatenated(result, args);
        return result.toString();
    }

    private void appendConcatenated(StringBuilder result, List<?> values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> nested) {
                appendConcatenated(result, nested);
            } else {
                result.append(javaScriptString(value));
            }
        }
    }

    private Object ifFunction(List<Supplier<Object>> args) {
        if (args.size() < 2) {
            return null;
        }
        if (toBoolean(args.get(0).get())) {
            return args.get(1).get();
        }
        return args.size() > 2 ? args.get(2).get() : null;
    }

    private Object ifs(List<Supplier<Object>> args) {
        for (int index = 0; index + 1 < args.size(); index += 2) {
            if (toBoolean(args.get(index).get())) {
                return args.get(index + 1).get();
            }
        }
        return null;
    }

    private boolean and(List<Supplier<Object>> args) {
        for (Supplier<Object> arg : args) {
            if (!toBoolean(arg.get())) {
                return false;
            }
        }
        return true;
    }

    private String text(List<Object> args) {
        if (args.size() < 2) {
            return "";
        }
        return formatTextValue(args.get(0), args.get(1));
    }

    private String formatTextValue(Object value, Object formatValue) {
        if (value == null || "".equals(value)) {
            return "";
        }
        String format = excelString(formatValue).trim();
        if (value instanceof Number && format.isEmpty()) {
            return "";
        }
        if (format.isEmpty()) {
            return excelString(value);
        }
        if (isDateFormat(format)) {
            double serial = toNumber(value);
            return Double.isNaN(serial) ? "" : formatDate(serial, format);
        }
        double number = toNumber(value);
        return Double.isNaN(number) ? excelString(value) : formatNumber(number, format);
    }

    private String formatDate(double serial, String format) {
        LocalDateTime date = excelSerialToLocalDateTime(serial);
        String result = format;
        String year = String.format(Locale.ROOT, "%04d", date.getYear());
        String month = String.format(Locale.ROOT, "%02d", date.getMonthValue());
        String day = String.format(Locale.ROOT, "%02d", date.getDayOfMonth());
        String hour = String.format(Locale.ROOT, "%02d", date.getHour());
        String minute = String.format(Locale.ROOT, "%02d", date.getMinute());
        String second = String.format(Locale.ROOT, "%02d", date.getSecond());

        result = result.replace("yyyy", year).replace("dd", day)
                .replace("hh", hour).replace("ss", second);
        result = result.replace(hour + ":mm", hour + ":" + minute)
                .replace(":mm:", ":" + minute + ":")
                .replaceAll(":mm\\b", ":" + minute);
        return result.replace("mm", month);
    }

    private String formatNumber(double value, String format) {
        if (!Double.isFinite(value)) {
            return "";
        }
        if (format.matches("^0+$")) {
            return String.format(Locale.US, "%0" + format.length() + "d", (long) value);
        }
        boolean percent = format.contains("%");
        double formattedValue = percent ? value * 100D : value;
        int decimalPlaces = decimalPlaces(format);
        String result = String.format(Locale.US, "%,." + decimalPlaces + "f", formattedValue);
        if (!format.contains(",")) {
            result = result.replace(",", "");
        }
        return result + (percent ? "%" : "");
    }

    private int decimalPlaces(String format) {
        int dot = format.indexOf('.');
        if (dot < 0) {
            return 0;
        }
        int result = 0;
        for (int index = dot + 1; index < format.length() && format.charAt(index) == '0'; index++) {
            result++;
        }
        return result;
    }

    private boolean isDateFormat(String format) {
        return format.toLowerCase(Locale.ROOT).matches(".*[ymdhs].*");
    }

    private double today(LocalDateTime now) {
        return Math.floor(localDateTimeToExcelSerial(now.toLocalDate().atStartOfDay()));
    }

    private double parseDateSerial(Object rawValue, ZoneId zoneId) {
        if (rawValue instanceof Number number) {
            double value = number.doubleValue();
            if (value > 1E10) {
                return localDateTimeToExcelSerial(
                        Instant.ofEpochMilli(number.longValue()).atZone(zoneId).toLocalDateTime());
            }
            return value;
        }
        if (rawValue instanceof LocalDateTime dateTime) {
            return localDateTimeToExcelSerial(dateTime);
        }
        if (rawValue instanceof LocalDate date) {
            return localDateTimeToExcelSerial(date.atStartOfDay());
        }
        String text = String.valueOf(rawValue);
        try {
            if (text.matches("^\\d{4}-\\d{2}$")) {
                return localDateTimeToExcelSerial(YearMonth.parse(text).atDay(1).atStartOfDay());
            }
            if (text.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return localDateTimeToExcelSerial(LocalDate.parse(text).atStartOfDay());
            }
            if (text.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")) {
                return localDateTimeToExcelSerial(LocalDateTime.parse(
                        text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            if (text.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
                return localDateTimeToExcelSerial(LocalDateTime.parse(
                        text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } catch (DateTimeParseException ignored) {
            return 0D;
        }
        return 0D;
    }

    private double localDateTimeToExcelSerial(LocalDateTime value) {
        return Duration.between(EXCEL_EPOCH, value).toMillis() / DAY_MILLIS;
    }

    private LocalDateTime excelSerialToLocalDateTime(double serial) {
        return EXCEL_EPOCH.plusNanos(Math.round(serial * DAY_MILLIS * 1_000_000D));
    }

    private Object javaScriptAdd(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return javaScriptString(left) + javaScriptString(right);
        }
        double leftNumber = toNumber(left);
        double rightNumber = toNumber(right);
        return leftNumber + rightNumber;
    }

    private Object numericBinary(
            Object left,
            Object right,
            FormulaEvaluationContext context,
            String operation,
            DoubleBinaryOperator operator
    ) {
        double leftNumber = toNumber(left);
        double rightNumber = toNumber(right);
        if (Double.isNaN(leftNumber) || Double.isNaN(rightNumber)) {
            context.warn("INVALID_" + operation + "_OPERANDS", left + ", " + right);
            return 0D;
        }
        return operator.apply(leftNumber, rightNumber);
    }

    private boolean excelCompare(Object left, Object right, String operator) {
        return switch (operator) {
            case "=" -> compareEqual(left, right);
            case "<>" -> !compareEqual(left, right);
            case ">" -> compareNumber(left, right, value -> value > 0);
            case ">=" -> compareNumber(left, right, value -> value >= 0);
            case "<" -> compareNumber(left, right, value -> value < 0);
            case "<=" -> compareNumber(left, right, value -> value <= 0);
            default -> false;
        };
    }

    private boolean compareEqual(Object left, Object right) {
        Object normalizedLeft = normalizeScalar(left);
        Object normalizedRight = normalizeScalar(right);
        boolean leftEmpty = normalizedLeft == null || "".equals(normalizedLeft);
        boolean rightEmpty = normalizedRight == null || "".equals(normalizedRight);
        if (leftEmpty && rightEmpty) {
            return true;
        }
        if (normalizedLeft instanceof Boolean || normalizedRight instanceof Boolean) {
            return toBoolean(normalizedLeft) == toBoolean(normalizedRight);
        }
        if (normalizedLeft instanceof Number || normalizedRight instanceof Number) {
            double leftNumber = toNumber(normalizedLeft);
            double rightNumber = toNumber(normalizedRight);
            if (!Double.isNaN(leftNumber) && !Double.isNaN(rightNumber)) {
                return Double.compare(leftNumber, rightNumber) == 0;
            }
        }
        return java.util.Objects.equals(normalizedLeft, normalizedRight);
    }

    private boolean compareNumber(Object left, Object right, IntPredicate predicate) {
        double leftNumber = toNumber(left);
        double rightNumber = toNumber(right);
        if (Double.isNaN(leftNumber) || Double.isNaN(rightNumber)) {
            return false;
        }
        return predicate.test(Double.compare(leftNumber, rightNumber));
    }

    private Object normalizeScalar(Object value) {
        if (value instanceof List<?> list) {
            return list.isEmpty() ? 0D : list.getFirst();
        }
        return value;
    }

    private boolean toBoolean(Object value) {
        Object scalar = normalizeScalar(value);
        if (scalar == null) {
            return false;
        }
        if (scalar instanceof Boolean bool) {
            return bool;
        }
        if (scalar instanceof Number number) {
            return number.doubleValue() != 0D;
        }
        if (scalar instanceof String text) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || "false".equals(normalized)) {
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean toJavaScriptBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null;
    }

    private double toNumber(Object value) {
        Object scalar = normalizeScalar(value);
        if (scalar == null) {
            return 0D;
        }
        if (scalar instanceof Number number) {
            return number.doubleValue();
        }
        if (scalar instanceof Boolean bool) {
            return bool ? 1D : 0D;
        }
        if (scalar instanceof String text) {
            if (text.trim().isEmpty()) {
                return 0D;
            }
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private double parseFieldNumber(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        String text = String.valueOf(rawValue).replace(",", "").replace("%", "");
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private double finiteNumberOrZero(Object value) {
        double result = parseFieldNumber(value);
        return Double.isNaN(result) ? 0D : result;
    }

    private String excelString(Object value) {
        Object scalar = normalizeScalar(value);
        if (scalar == null) {
            return "";
        }
        if (scalar instanceof Boolean bool) {
            return bool ? "TRUE" : "FALSE";
        }
        return javaScriptString(scalar);
    }

    private String javaScriptString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Double number) {
            if (number.isNaN()) {
                return "NaN";
            }
            if (number == Math.rint(number)) {
                return Long.toString(number.longValue());
            }
        }
        if (value instanceof Float number && number == Math.rint(number)) {
            return Long.toString(number.longValue());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private double normalizeFormulaNumber(double value) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return BigDecimal.valueOf(value).round(FORMULA_PRECISION).doubleValue();
    }

    @FunctionalInterface
    private interface DoubleBinaryOperator {
        double apply(double left, double right);
    }

    @FunctionalInterface
    private interface IntPredicate {
        boolean test(int value);
    }
}
