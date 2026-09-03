package cn.cordys.common.formula;

import java.util.List;

/**
 * 前端公式编辑器持久化 IR 的 Java 表示。
 */
public sealed interface FormulaNode permits FormulaNode.Literal, FormulaNode.Field,
        FormulaNode.Binary, FormulaNode.Compare, FormulaNode.Function, FormulaNode.Invalid {

    record Literal(Object value, String valueType) implements FormulaNode {
    }

    record Field(String fieldId) implements FormulaNode {
    }

    record Binary(String operator, FormulaNode left, FormulaNode right) implements FormulaNode {
    }

    record Compare(String operator, FormulaNode left, FormulaNode right) implements FormulaNode {
    }

    record Function(String name, List<FormulaNode> args) implements FormulaNode {
        public Function {
            args = List.copyOf(args);
        }
    }

    record Invalid(String reason) implements FormulaNode {
    }
}
