package cn.cordys.common.formula;

import cn.cordys.common.util.JSON;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaEngineTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 2, 17, 9, 11);

    private final FormulaEngine engine = new FormulaEngine();
    private final FormulaEvaluationContext emptyContext = context(Map.of(), Map.of());

    @Nested
    @DisplayName("SUM")
    class SumTests {

        @Test
        void sumsScalarAndTableColumnNumbers() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("amount", 2);
            values.put("items", List.of(Map.of("price", 3), Map.of("price", 4)));
            Map<String, FormulaFieldMetadata> metadata = Map.of(
                    "amount", numberMeta("金额"),
                    "items.price", numberMeta("明细.价格")
            );

            Object result = evaluate(fn("SUM", field("amount"), field("items.price")), context(values, metadata));

            assertEquals(9D, result);
        }

        @Test
        void ignoresTextAndEmptyValues() {
            Object result = evaluate(fn("SUM", literal("12", "string"), literal(null, "string")), emptyContext);

            assertEquals(0D, result);
        }
    }

    @Nested
    @DisplayName("DAYS")
    class DaysTests {

        @Test
        void returnsWholeDayDifference() {
            Object result = evaluate(fn("DAYS", literal(46000.9, "number"), literal(45998.1, "number")), emptyContext);

            assertEquals(2D, result);
        }

        @Test
        void returnsZeroForNonNumericArguments() {
            Object result = evaluate(fn("DAYS", literal("bad", "string"), literal(45998, "number")), emptyContext);

            assertEquals(0D, result);
        }
    }

    @Nested
    @DisplayName("CONCATENATE")
    class ConcatenateTests {

        @Test
        void concatenatesTextNumbersAndArrayValues() {
            Object result = evaluate(fn("CONCATENATE",
                    literal("报价", "string"), literal("-", "string"), literal(12, "number")), emptyContext);

            assertEquals("报价-12", result);
        }

        @Test
        void skipsNullAndReturnsEmptyForNoEffectiveValue() {
            Object result = evaluate(fn("CONCATENATE", literal(null, "string")), emptyContext);

            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("TEXT")
    class TextTests {

        @Test
        void formatsNumberWithThousandsAndDecimals() {
            Object result = evaluate(fn("TEXT", literal(1234.5, "number"), literal("#,##0.00", "string")), emptyContext);

            assertEquals("1,234.50", result);
        }

        @Test
        void returnsEmptyForInvalidDateValue() {
            Object result = evaluate(fn("TEXT", literal("not-a-date", "string"), literal("yyyy-mm-dd", "string")), emptyContext);

            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("IF")
    class IfTests {

        @Test
        void returnsTrueBranch() {
            Object result = evaluate(fn("IF", literal(true, "boolean"),
                    literal("yes", "string"), literal("no", "string")), emptyContext);

            assertEquals("yes", result);
        }

        @Test
        void returnsNullWhenFalseBranchIsMissing() {
            Object result = evaluate(fn("IF", literal(false, "boolean"), literal("yes", "string")), emptyContext);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("IFS")
    class IfsTests {

        @Test
        void returnsFirstMatchingBranch() {
            Object result = evaluate(fn("IFS",
                    literal(false, "boolean"), literal("first", "string"),
                    literal(true, "boolean"), literal("second", "string"),
                    literal(true, "boolean"), literal("third", "string")), emptyContext);

            assertEquals("second", result);
        }

        @Test
        void returnsNullWhenNoConditionMatches() {
            Object result = evaluate(fn("IFS",
                    literal(false, "boolean"), literal("first", "string"),
                    literal(false, "boolean"), literal("second", "string")), emptyContext);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("AND")
    class AndTests {

        @Test
        void returnsTrueWhenEveryArgumentIsTruthy() {
            Object result = evaluate(fn("AND", literal(true, "boolean"), literal(1, "number")), emptyContext);

            assertTrue((Boolean) result);
        }

        @Test
        void returnsFalseWhenAnyArgumentIsFalsy() {
            Object result = evaluate(fn("AND", literal(true, "boolean"), literal(0, "number")), emptyContext);

            assertFalse((Boolean) result);
        }
    }

    @Nested
    @DisplayName("TODAY")
    class TodayTests {

        @Test
        void returnsCurrentLocalDateAsExcelSerial() {
            Object result = evaluate(fn("TODAY"), emptyContext);

            assertEquals(excelDay(LocalDate.of(2026, 9, 2)), result);
        }

        @Test
        void ignoresUnexpectedRuntimeArgumentsLikeFrontend() {
            Object result = evaluate(fn("TODAY", literal(123, "number")), emptyContext);

            assertEquals(excelDay(LocalDate.of(2026, 9, 2)), result);
        }
    }

    @Nested
    @DisplayName("NOW")
    class NowTests {

        @Test
        void returnsCurrentLocalDateTimeAsExcelSerial() {
            double expected = excelDay(NOW.toLocalDate())
                    + (17 * 3600 + 9 * 60 + 11) / 86400D;

            Object result = evaluate(fn("NOW"), emptyContext);

            assertEquals(expected, (Double) result, 1E-10);
        }

        @Test
        void ignoresUnexpectedRuntimeArgumentsLikeFrontend() {
            Object withoutArgs = evaluate(fn("NOW"), emptyContext);
            Object withArgs = evaluate(fn("NOW", literal("ignored", "string")), emptyContext);

            assertEquals((Double) withoutArgs, (Double) withArgs, 0D);
        }
    }

    @Test
    void evaluatesArithmeticComparisonAndDivisionByZeroLikeFrontend() {
        Object arithmetic = evaluate(binary("*", literal(5, "number"), literal(4, "number")), emptyContext);
        Object comparison = evaluate(compare(">=", literal(20, "number"), literal("20", "string")), emptyContext);
        Object divisionByZero = evaluate(binary("/", literal(5, "number"), literal(0, "number")), emptyContext);

        assertEquals(20D, arithmetic);
        assertTrue((Boolean) comparison);
        assertEquals(0D, divisionByZero);
    }

    @Test
    void returnsNullForUnknownFunctionAndCollectsWarning() {
        List<String> warnings = new java.util.ArrayList<>();
        FormulaEvaluationContext context = new FormulaEvaluationContext(
                Map.of(), Map.of(), Map.of(), NOW, (fieldId, value) -> value,
                (code, message) -> warnings.add(code), true);

        Object result = evaluate(fn("NOT_SUPPORTED"), context);

        assertNull(result);
        assertEquals(List.of("UNKNOWN_FUNCTION"), warnings);
    }

    private Object evaluate(Map<String, Object> ir, FormulaEvaluationContext context) {
        return engine.evaluate(formula(ir), context);
    }

    private FormulaEvaluationContext context(
            Map<String, Object> values,
            Map<String, FormulaFieldMetadata> metadata
    ) {
        return new FormulaEvaluationContext(values, metadata, NOW);
    }

    private FormulaFieldMetadata numberMeta(String name) {
        return new FormulaFieldMetadata(name, "INPUT_NUMBER",
                FormulaFieldMetadata.ValueType.NUMBER,
                FormulaFieldMetadata.NumberType.NUMBER, false);
    }

    private double excelDay(LocalDate date) {
        return ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), date);
    }

    private String formula(Map<String, Object> ir) {
        return JSON.toJSONString(Map.of(
                "source", "test",
                "display", "test",
                "fields", List.of(),
                "ir", ir
        ));
    }

    private Map<String, Object> literal(Object value, String valueType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "literal");
        result.put("value", value);
        result.put("valueType", valueType);
        return result;
    }

    private Map<String, Object> field(String fieldId) {
        return Map.of("type", "field", "fieldId", fieldId);
    }

    @SafeVarargs
    private Map<String, Object> fn(String name, Map<String, Object>... args) {
        return Map.of("type", "function", "name", name, "args", List.of(args));
    }

    private Map<String, Object> binary(String operator, Map<String, Object> left, Map<String, Object> right) {
        return Map.of("type", "binary", "operator", operator, "left", left, "right", right);
    }

    private Map<String, Object> compare(String operator, Map<String, Object> left, Map<String, Object> right) {
        return Map.of("type", "compare", "operator", operator, "left", left, "right", right);
    }
}
