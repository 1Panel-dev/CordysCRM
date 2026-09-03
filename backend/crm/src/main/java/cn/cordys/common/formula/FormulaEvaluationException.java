package cn.cordys.common.formula;

/**
 * 表单公式定义无法安全求值时抛出的稳定异常。
 */
public class FormulaEvaluationException extends RuntimeException {

    public FormulaEvaluationException(String message) {
        super(message);
    }
}
