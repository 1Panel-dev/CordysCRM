package cn.cordys.common.formula;

import cn.cordys.common.util.JSON;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormulaDefinitionValidationTest {
    private final FormulaEngine engine = new FormulaEngine();

    @Test
    void acceptsKnownReferenceAndLegalEmptyText() {
        assertDoesNotThrow(() -> validate(Map.of("type", "field", "fieldId", "amount")));
        assertDoesNotThrow(() -> validate(Map.of("type", "literal", "valueType", "string", "value", "")));
    }

    @Test
    void rejectsDanglingReferenceAndInvalidJson() {
        assertThrows(FormulaEvaluationException.class,
                () -> validate(Map.of("type", "field", "fieldId", "deleted")));
        assertThrows(FormulaEvaluationException.class,
                () -> engine.validateDefinition("broken", Set.of()));
    }

    @Test
    void rejectsInvalidNodeEvenInUnselectedBranch() {
        assertThrows(FormulaEvaluationException.class, () -> validate(Map.of(
                "type", "function", "name", "IF", "args", List.of(
                        Map.of("type", "literal", "valueType", "boolean", "value", true),
                        Map.of("type", "literal", "valueType", "string", "value", "ok"),
                        Map.of("type", "invalid", "reason", "unfinished")))));
    }

    @Test
    void rejectsUnknownFunctionAndWrongArity() {
        assertThrows(FormulaEvaluationException.class,
                () -> validate(Map.of("type", "function", "name", "EXEC", "args", List.of())));
        assertThrows(FormulaEvaluationException.class,
                () -> validate(Map.of("type", "function", "name", "DAYS", "args", List.of())));
    }

    private void validate(Map<String, Object> ir) {
        engine.validateDefinition(JSON.toJSONString(Map.of("ir", ir)), Set.of("amount"));
    }

    @Test
    void rejectsWrongLiteralTypesAndNonArrayArgumentsInLazyBranches() {
        for (Map<String, Object> invalid : List.of(
                Map.<String, Object>of("type", "literal", "valueType", "number", "value", "not-number"),
                Map.<String, Object>of("type", "literal", "valueType", "boolean", "value", Map.of()),
                Map.<String, Object>of("type", "literal", "valueType", "string", "value", 1),
                Map.<String, Object>of("type", "function", "name", "NOW", "args", Map.of()))) {
            assertThrows(FormulaEvaluationException.class, () -> validate(invalid));
            assertThrows(FormulaEvaluationException.class, () -> validate(Map.of(
                    "type", "function", "name", "IF", "args", List.of(
                            Map.of("type", "literal", "valueType", "boolean", "value", true),
                            Map.of("type", "literal", "valueType", "number", "value", 1), invalid))));
        }
        assertDoesNotThrow(() -> validate(Map.of("type", "function", "name", "NOW", "args", List.of())));
    }
}
