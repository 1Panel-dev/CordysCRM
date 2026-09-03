package cn.cordys.common.formula;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 核心求值器的内部上下文。所有内容由服务端根据实时表单构造，不进入 MCP Tool Schema。
 */
public final class FormulaEvaluationContext {

    @FunctionalInterface
    public interface DisplayValueResolver {
        Object resolve(String fieldId, Object rawValue);
    }

    private final Map<String, Object> values;
    private final Map<String, Object> rowValues;
    private final Map<String, FormulaFieldMetadata> metadata;
    private final LocalDateTime evaluationNow;
    private final DisplayValueResolver displayValueResolver;
    private final BiConsumer<String, String> warningConsumer;
    private final boolean createMode;

    public FormulaEvaluationContext(
            Map<String, Object> values,
            Map<String, FormulaFieldMetadata> metadata,
            LocalDateTime evaluationNow
    ) {
        this(values, Collections.emptyMap(), metadata, evaluationNow,
                (fieldId, value) -> value, (code, message) -> { }, true);
    }

    public FormulaEvaluationContext(
            Map<String, Object> values,
            Map<String, Object> rowValues,
            Map<String, FormulaFieldMetadata> metadata,
            LocalDateTime evaluationNow,
            DisplayValueResolver displayValueResolver,
            BiConsumer<String, String> warningConsumer,
            boolean createMode
    ) {
        this.values = Objects.requireNonNullElse(values, Collections.emptyMap());
        this.rowValues = Objects.requireNonNullElse(rowValues, Collections.emptyMap());
        this.metadata = Objects.requireNonNullElse(metadata, Collections.emptyMap());
        this.evaluationNow = Objects.requireNonNull(evaluationNow);
        this.displayValueResolver = Objects.requireNonNull(displayValueResolver);
        this.warningConsumer = Objects.requireNonNull(warningConsumer);
        this.createMode = createMode;
    }

    public FormulaEvaluationContext withRow(Map<String, Object> currentRow) {
        return new FormulaEvaluationContext(values, currentRow, metadata, evaluationNow,
                displayValueResolver, warningConsumer, createMode);
    }

    public Object scalarValue(String fieldId) {
        return rowValues.containsKey(fieldId) ? rowValues.get(fieldId) : values.get(fieldId);
    }

    public Object value(String fieldId) {
        return values.get(fieldId);
    }

    public FormulaFieldMetadata metadata(String fieldId) {
        return metadata.get(fieldId);
    }

    public LocalDateTime evaluationNow() {
        return evaluationNow;
    }

    public Object resolveDisplayValue(String fieldId, Object rawValue) {
        return displayValueResolver.resolve(fieldId, rawValue);
    }

    public void warn(String code, String message) {
        warningConsumer.accept(code, message);
    }

    public boolean createMode() {
        return createMode;
    }
}
