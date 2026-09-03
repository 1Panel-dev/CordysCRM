package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 在业务保存前根据实时表单补全公式字段。调用方只提供当前资源和字段值，公式及类型均来自表单定义。
 */
@Slf4j
@Service
public class FormulaCompletionService {

    @FunctionalInterface
    interface DisplayValueResolver {
        Object resolve(BaseField field, Object rawValue);
    }

    private final FormulaEngine formulaEngine;
    private final FormulaResultNormalizer resultNormalizer;
    private final Clock clock;
    private final DisplayValueResolver displayValueResolver;

    @Autowired
    public FormulaCompletionService(FormulaEngine formulaEngine) {
        this(formulaEngine, new FormulaResultNormalizer(), Clock.systemDefaultZone(),
                FormulaCompletionService::resolveDisplayValue);
    }

    FormulaCompletionService(
            FormulaEngine formulaEngine,
            FormulaResultNormalizer resultNormalizer,
            Clock clock,
            DisplayValueResolver displayValueResolver
    ) {
        this.formulaEngine = formulaEngine;
        this.resultNormalizer = resultNormalizer;
        this.clock = clock;
        this.displayValueResolver = displayValueResolver;
    }

    /**
     * 计算并覆盖所有公式字段。businessValueReader/writer 负责访问不同业务实体上的内置字段。
     */
    public Map<String, Object> complete(
            List<BaseField> fields,
            Map<String, BaseModuleFieldValue> fieldValueMap,
            boolean createMode,
            Function<String, Object> businessValueReader,
            BiConsumer<String, Object> businessValueWriter
    ) {
        return complete(fields, fieldValueMap, createMode, businessValueReader,
                businessValueWriter, true);
    }

    /**
     * 只计算当前没有值的公式字段。用于 HTTP 请求前置补全：前端已经计算的值保持不变，
     * MCP 等未提交公式值的调用方才由服务端补齐。
     */
    public Map<String, Object> completeMissing(
            List<BaseField> fields,
            Map<String, BaseModuleFieldValue> fieldValueMap,
            boolean createMode,
            Function<String, Object> businessValueReader,
            BiConsumer<String, Object> businessValueWriter
    ) {
        return complete(fields, fieldValueMap, createMode, businessValueReader,
                businessValueWriter, false);
    }

    private Map<String, Object> complete(
            List<BaseField> fields,
            Map<String, BaseModuleFieldValue> fieldValueMap,
            boolean createMode,
            Function<String, Object> businessValueReader,
            BiConsumer<String, Object> businessValueWriter,
            boolean overwriteExisting
    ) {
        Map<String, Object> calculatedValues = new LinkedHashMap<>();
        if (fields == null || fields.isEmpty()) {
            return calculatedValues;
        }

        LocalDateTime evaluationNow = LocalDateTime.now(clock);
        Map<String, BaseField> runtimeFieldMap = buildRuntimeFieldMap(fields);
        Map<String, FormulaFieldMetadata> metadata = buildMetadata(runtimeFieldMap);
        Map<String, Object> runtimeValues = buildRuntimeValues(fields, fieldValueMap, businessValueReader);

        calculateSubTableFormulas(fields, runtimeValues, metadata, runtimeFieldMap,
                calculatedValues, evaluationNow, createMode, overwriteExisting);
        calculateTopLevelFormulas(fields, runtimeValues, metadata, runtimeFieldMap,
                calculatedValues, evaluationNow, createMode, fieldValueMap,
                businessValueWriter, overwriteExisting);
        return calculatedValues;
    }

    @SuppressWarnings("unchecked")
    private void calculateSubTableFormulas(
            List<BaseField> fields,
            Map<String, Object> runtimeValues,
            Map<String, FormulaFieldMetadata> metadata,
            Map<String, BaseField> runtimeFieldMap,
            Map<String, Object> calculatedValues,
            LocalDateTime evaluationNow,
            boolean createMode,
            boolean overwriteExisting
    ) {
        for (BaseField field : fields) {
            if (!(field instanceof SubField subField) || subField.getSubFields() == null) {
                continue;
            }
            Object tableValue = runtimeValues.get(field.getId());
            if (!(tableValue instanceof List<?> rawRows)) {
                continue;
            }
            List<FormulaTarget> targets = subField.getSubFields().stream()
                    .map(sub -> formulaTarget(sub, runtimeFieldId(sub)))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            List<FormulaTarget> sortedTargets = sortTargets(targets);
            for (Object rawRow : rawRows) {
                if (!(rawRow instanceof Map<?, ?>)) {
                    continue;
                }
                Map<String, Object> row = (Map<String, Object>) rawRow;
                Map<String, FormulaFieldMetadata> rowMetadata = new HashMap<>(metadata);
                Map<String, BaseField> rowFieldMap = new HashMap<>(runtimeFieldMap);
                for (BaseField sub : subField.getSubFields()) {
                    String runtimeId = runtimeFieldId(sub);
                    rowMetadata.put(runtimeId, toMetadata(sub));
                    rowFieldMap.put(runtimeId, sub);
                }
                FormulaEvaluationContext baseContext = context(runtimeValues, rowMetadata,
                        rowFieldMap, evaluationNow, createMode);
                for (FormulaTarget target : sortedTargets) {
                    if (!overwriteExisting && row.get(target.runtimeId()) != null) {
                        continue;
                    }
                    Object rawResult = formulaEngine.evaluate(target.formula(), baseContext.withRow(row));
                    Object normalized = normalizeResult(rawResult, target.field());
                    row.put(target.runtimeId(), normalized);
                    calculatedValues.put(field.getId() + "." + target.runtimeId(), normalized);
                }
            }
        }
    }

    private void calculateTopLevelFormulas(
            List<BaseField> fields,
            Map<String, Object> runtimeValues,
            Map<String, FormulaFieldMetadata> metadata,
            Map<String, BaseField> runtimeFieldMap,
            Map<String, Object> calculatedValues,
            LocalDateTime evaluationNow,
            boolean createMode,
            Map<String, BaseModuleFieldValue> fieldValueMap,
            BiConsumer<String, Object> businessValueWriter,
            boolean overwriteExisting
    ) {
        List<FormulaTarget> targets = fields.stream()
                .filter(field -> !(field instanceof SubField))
                .map(field -> formulaTarget(field, field.getId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        FormulaEvaluationContext context = context(runtimeValues, metadata,
                runtimeFieldMap, evaluationNow, createMode);
        for (FormulaTarget target : sortTargets(targets)) {
            if (!overwriteExisting && runtimeValues.get(target.runtimeId()) != null) {
                continue;
            }
            Object rawResult = formulaEngine.evaluate(target.formula(), context);
            Object normalized = normalizeResult(rawResult, target.field());
            runtimeValues.put(target.runtimeId(), normalized);
            calculatedValues.put(target.runtimeId(), normalized);
            if (StringUtils.isNotBlank(target.field().getBusinessKey())) {
                businessValueWriter.accept(target.field().getBusinessKey(), normalized);
            } else {
                fieldValueMap.put(target.field().getId(),
                        new BaseModuleFieldValue(target.field().getId(), normalized));
            }
        }
    }

    private FormulaEvaluationContext context(
            Map<String, Object> runtimeValues,
            Map<String, FormulaFieldMetadata> metadata,
            Map<String, BaseField> runtimeFieldMap,
            LocalDateTime evaluationNow,
            boolean createMode
    ) {
        return new FormulaEvaluationContext(runtimeValues, Map.of(), metadata,
                evaluationNow,
                (fieldId, rawValue) -> {
                    BaseField field = runtimeFieldMap.get(fieldId);
                    return field == null ? rawValue : displayValueResolver.resolve(field, rawValue);
                },
                (code, message) -> log.warn("Formula evaluation warning: code={}, detail={}", code, message),
                createMode);
    }

    private Map<String, Object> buildRuntimeValues(
            List<BaseField> fields,
            Map<String, BaseModuleFieldValue> fieldValueMap,
            Function<String, Object> businessValueReader
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (BaseField field : fields) {
            Object value;
            if (StringUtils.isNotBlank(field.getBusinessKey())) {
                value = businessValueReader.apply(field.getBusinessKey());
            } else {
                BaseModuleFieldValue fieldValue = fieldValueMap.get(field.getId());
                value = fieldValue == null ? null : fieldValue.getFieldValue();
            }
            result.put(field.getId(), value);
        }
        return result;
    }

    private Map<String, BaseField> buildRuntimeFieldMap(List<BaseField> fields) {
        Map<String, BaseField> result = new LinkedHashMap<>();
        for (BaseField field : fields) {
            result.put(field.getId(), field);
            if (field instanceof SubField subField && subField.getSubFields() != null) {
                for (BaseField sub : subField.getSubFields()) {
                    String runtimeId = runtimeFieldId(sub);
                    result.put(field.getId() + "." + runtimeId, sub);
                }
            }
        }
        return result;
    }

    private Map<String, FormulaFieldMetadata> buildMetadata(Map<String, BaseField> runtimeFieldMap) {
        Map<String, FormulaFieldMetadata> result = new LinkedHashMap<>();
        runtimeFieldMap.forEach((fieldId, field) -> result.put(fieldId, toMetadata(field)));
        return result;
    }

    private FormulaFieldMetadata toMetadata(BaseField field) {
        FormulaFieldMetadata.ValueType valueType = switch (field.getType()) {
            case "INPUT_NUMBER" -> FormulaFieldMetadata.ValueType.NUMBER;
            case "DATE_TIME" -> FormulaFieldMetadata.ValueType.DATE;
            case "INPUT", "DATA_SOURCE", "DATA_SOURCE_MULTIPLE", "SERIAL_NUMBER", "SELECT" ->
                    FormulaFieldMetadata.ValueType.STRING;
            case "RADIO", "CHECKBOX" -> FormulaFieldMetadata.ValueType.BOOLEAN;
            default -> FormulaFieldMetadata.ValueType.UNKNOWN;
        };
        FormulaFieldMetadata.NumberType numberType = field instanceof InputNumberField numberField
                && Strings.CI.equals(numberField.getNumberFormat(), "percent")
                ? FormulaFieldMetadata.NumberType.PERCENT
                : FormulaFieldMetadata.NumberType.NUMBER;
        return new FormulaFieldMetadata(field.getName(), field.getType(), valueType,
                numberType, StringUtils.isNotBlank(field.getResourceFieldId()));
    }

    private FormulaTarget formulaTarget(BaseField field, String runtimeId) {
        String formula = formulaOf(field);
        return StringUtils.isBlank(formula) ? null : new FormulaTarget(field, runtimeId, formula);
    }

    private String formulaOf(BaseField field) {
        if (field instanceof FormulaField formulaField) {
            return formulaField.getFormula();
        }
        if (field instanceof InputField inputField
                && Strings.CI.equals(inputField.getDefaultValueType(), "formula")) {
            return inputField.getFormula();
        }
        return null;
    }

    private String runtimeFieldId(BaseField field) {
        return StringUtils.isNotBlank(field.getBusinessKey()) ? field.getBusinessKey() : field.getId();
    }

    private Object normalizeResult(Object rawResult, BaseField field) {
        if (field instanceof FormulaField formulaField) {
            boolean numberResult = Strings.CI.equals(formulaField.getFormulaResultFormat(), "number");
            int decimalPlaces = numberResult && Boolean.TRUE.equals(formulaField.getDecimalPlaces())
                    ? formulaField.getPrecision() : 0;
            return resultNormalizer.normalize(rawResult, decimalPlaces,
                    numberResult
                            ? FormulaResultNormalizer.ExpectedType.UNSPECIFIED
                            : FormulaResultNormalizer.ExpectedType.STRING);
        }
        Object normalized = resultNormalizer.normalize(rawResult, 2,
                FormulaResultNormalizer.ExpectedType.UNSPECIFIED);
        return resultNormalizer.javaScriptString(normalized);
    }

    private List<FormulaTarget> sortTargets(List<FormulaTarget> targets) {
        Map<String, FormulaTarget> targetMap = new LinkedHashMap<>();
        targets.forEach(target -> targetMap.put(target.runtimeId(), target));
        List<FormulaTarget> result = new ArrayList<>(targets.size());
        Map<String, VisitState> states = new HashMap<>();
        for (FormulaTarget target : targets) {
            visit(target, targetMap, states, result, new LinkedHashSet<>());
        }
        return result;
    }

    private void visit(
            FormulaTarget target,
            Map<String, FormulaTarget> targetMap,
            Map<String, VisitState> states,
            List<FormulaTarget> result,
            Set<String> path
    ) {
        VisitState state = states.get(target.runtimeId());
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            path.add(target.runtimeId());
            throw new FormulaEvaluationException("公式字段存在循环依赖: " + String.join(" -> ", path));
        }
        states.put(target.runtimeId(), VisitState.VISITING);
        path.add(target.runtimeId());
        for (String dependency : formulaEngine.referencedFieldIds(target.formula())) {
            FormulaTarget dependencyTarget = targetMap.get(dependency);
            if (dependencyTarget != null) {
                visit(dependencyTarget, targetMap, states, result, new LinkedHashSet<>(path));
            }
        }
        states.put(target.runtimeId(), VisitState.VISITED);
        result.add(target);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object resolveDisplayValue(BaseField field, Object rawValue) {
        if (rawValue == null || !Strings.CS.equalsAny(field.getType(),
                FieldType.DATA_SOURCE.name(), FieldType.DATA_SOURCE_MULTIPLE.name(), FieldType.SELECT.name())) {
            return rawValue;
        }
        try {
            AbstractModuleFieldResolver resolver = ModuleFieldResolverFactory.getResolver(field.getType());
            String storedValue = resolver.convertToString(field, rawValue);
            return resolver.transformToValue(field, storedValue);
        } catch (RuntimeException e) {
            return rawValue;
        }
    }

    private record FormulaTarget(BaseField field, String runtimeId, String formula) {
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
