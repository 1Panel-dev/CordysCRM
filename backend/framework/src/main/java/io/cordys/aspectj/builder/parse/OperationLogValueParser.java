package io.cordys.aspectj.builder.parse;

import io.cordys.aspectj.builder.MethodExecuteResult;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析需要存储的日志里面的SpeEL表达式
 */
public class OperationLogValueParser implements BeanFactoryAware {

    private static final Pattern pattern = Pattern.compile("\\{\\s*(\\w*)\\s*\\{(.*?)}}");
    private final OperationLogExpressionEvaluator expressionEvaluator = new OperationLogExpressionEvaluator();
    protected BeanFactory beanFactory;

    @Setter
    private OperationLogFunctionParser operationLogFunctionParser;


    public String singleProcessTemplate(MethodExecuteResult methodExecuteResult,
                                        String templates,
                                        Map<String, String> beforeFunctionNameAndReturnMap) {
        Map<String, String> stringStringMap = processTemplate(Collections.singletonList(templates), methodExecuteResult,
                beforeFunctionNameAndReturnMap);
        return stringStringMap.get(templates);
    }

    public Map<String, String> processTemplate(Collection<String> templates, MethodExecuteResult methodExecuteResult,
                                               Map<String, String> beforeFunctionNameAndReturnMap) {
        Map<String, String> expressionValues = new HashMap<>();
        EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(methodExecuteResult.getMethod(),
                methodExecuteResult.getArgs(), methodExecuteResult.getTargetClass(), methodExecuteResult.getResult(),
                methodExecuteResult.getErrorMsg(), beanFactory);

        for (String expressionTemplate : templates) {
            if (expressionTemplate.contains("{")) {
                Matcher matcher = pattern.matcher(expressionTemplate);
                StringBuilder parsedStr = new StringBuilder();
                AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(methodExecuteResult.getMethod(), methodExecuteResult.getTargetClass());
                while (matcher.find()) {
                    String expression = matcher.group(2);
                    String functionName = matcher.group(1);
                    Object value = expressionEvaluator.parseExpression(expression, annotatedElementKey, evaluationContext);
                    expression = operationLogFunctionParser.getFunctionReturnValue(beforeFunctionNameAndReturnMap, value, expression, functionName);

                    matcher.appendReplacement(parsedStr, Matcher.quoteReplacement(expression == null ? "" : expression));
                }
                matcher.appendTail(parsedStr);
                expressionValues.put(expressionTemplate, parsedStr.toString());
            } else {
                expressionValues.put(expressionTemplate, expressionTemplate);
            }

        }
        return expressionValues;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
