package io.cordys.aspectj.builder.parse;

import io.cordys.aspectj.context.OperationLogContext;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 自定义的日志记录表达式上下文类，继承自 {@link MethodBasedEvaluationContext}，用于处理方法日志记录时的上下文。
 * 该类在构造时会将方法参数、返回值、错误信息以及全局变量和当前上下文的变量注入到表达式上下文中，
 * 以便在日志记录时进行动态解析。
 */
public class OperationLogEvaluationContext extends MethodBasedEvaluationContext {

    /**
     * 构造方法，初始化日志记录表达式上下文。
     *
     * @param method                  被调用的方法
     * @param arguments               方法参数
     * @param parameterNameDiscoverer 参数名称发现器
     * @param ret                     方法返回值
     * @param errorMsg                错误信息
     */
    public OperationLogEvaluationContext(Object target,Method method, Object[] arguments,
                                         ParameterNameDiscoverer parameterNameDiscoverer, Object ret, String errorMsg) {
        // 调用父类构造方法初始化
        super(target, method, arguments, parameterNameDiscoverer);

        // 获取日志记录上下文中的变量
        Map<String, Object> variables = OperationLogContext.getVariables();
        Map<String, Object> globalVariables = OperationLogContext.getGlobalVariableMap();

        // 设置当前上下文中的变量
        setVariables(variables);

        // 如果全局变量不为空，将其添加到上下文中
        if (!globalVariables.isEmpty()) {
            for (Map.Entry<String, Object> entry : globalVariables.entrySet()) {
                // 如果当前上下文中没有该变量，则设置该变量
                if (lookupVariable(entry.getKey()) == null) {
                    setVariable(entry.getKey(), entry.getValue());
                }
            }
        }

        // 设置返回值和错误信息变量
        setVariable("_ret", ret);
        setVariable("_errorMsg", errorMsg);
    }
}
