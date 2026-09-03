package cn.cordys.common.formula;

import cn.cordys.common.util.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 解析前端保存的 {source, display, fields, ir}，运行时只信任封闭的 IR 节点集合。
 */
public class FormulaDefinitionParser {

    public FormulaNode parse(String formulaJson) {
        if (formulaJson == null || formulaJson.isBlank()) {
            return new FormulaNode.Invalid("formula is empty");
        }
        try {
            Map<String, Object> definition = JSON.parseToMap(formulaJson);
            return parseNode(asMap(definition.get("ir")));
        } catch (RuntimeException e) {
            return new FormulaNode.Invalid("formula json is invalid");
        }
    }

    private FormulaNode parseNode(Map<String, Object> source) {
        if (source.isEmpty()) {
            return new FormulaNode.Invalid("formula ir is empty");
        }
        String type = stringValue(source.get("type")).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "literal" -> new FormulaNode.Literal(source.get("value"), stringValue(source.get("valueType")));
            // 兼容前端 hydrateIRNumberType 处理的历史 IR。
            case "number", "string", "boolean" -> new FormulaNode.Literal(source.get("value"), type);
            case "field" -> new FormulaNode.Field(stringValue(source.get("fieldId")));
            case "binary" -> new FormulaNode.Binary(
                    stringValue(source.get("operator")),
                    parseNode(asMap(source.get("left"))),
                    parseNode(asMap(source.get("right")))
            );
            case "compare" -> new FormulaNode.Compare(
                    stringValue(source.get("operator")),
                    parseNode(asMap(source.get("left"))),
                    parseNode(asMap(source.get("right")))
            );
            case "function" -> new FormulaNode.Function(
                    stringValue(source.get("name")).toUpperCase(Locale.ROOT),
                    parseArgs(source.get("args"))
            );
            case "invalid" -> new FormulaNode.Invalid(stringValue(source.get("reason")));
            default -> new FormulaNode.Invalid("unknown formula node type: " + type);
        };
    }

    private List<FormulaNode> parseArgs(Object rawArgs) {
        if (!(rawArgs instanceof List<?> values)) {
            return List.of();
        }
        List<FormulaNode> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(parseNode(asMap(value)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
