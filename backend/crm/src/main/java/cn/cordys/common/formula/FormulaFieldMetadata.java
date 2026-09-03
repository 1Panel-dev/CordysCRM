package cn.cordys.common.formula;

/**
 * 公式运行时所需的最小字段元数据，不暴露为 AI 工具参数。
 */
public record FormulaFieldMetadata(
        String name,
        String fieldType,
        ValueType valueType,
        NumberType numberType,
        boolean referenceField
) {

    public enum ValueType {
        NUMBER,
        STRING,
        BOOLEAN,
        DATE,
        UNKNOWN
    }

    public enum NumberType {
        NUMBER,
        PERCENT
    }
}
