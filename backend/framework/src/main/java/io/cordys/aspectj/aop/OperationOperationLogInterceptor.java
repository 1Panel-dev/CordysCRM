package io.cordys.aspectj.aop;

import io.cordys.aspectj.builder.MethodExecuteResult;
import io.cordys.aspectj.builder.OperationLog;
import io.cordys.aspectj.builder.OperationLogBuilder;
import io.cordys.aspectj.builder.parse.OperationLogFunctionParser;
import io.cordys.aspectj.builder.parse.OperationLogValueParser;
import io.cordys.aspectj.constants.CodeVariableType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.handler.OperationLogService;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.LogUtils;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 日志记录拦截器，拦截方法执行并生成日志记录。
 * <p>
 * 该类支持基于注解的日志模板解析，能够在方法执行的前后记录业务操作日志。
 * </p>
 */
public class OperationOperationLogInterceptor extends OperationLogValueParser implements MethodInterceptor, Serializable, SmartInitializingSingleton {

    @Setter
    private OperationLogSource operationLogSource;

    private OperationLogService operationLogService;

    /**
     * 拦截方法执行，进行日志记录逻辑的处理。
     *
     * @param invocation 方法调用上下文
     * @return 方法执行结果
     * @throws Throwable 执行过程中的异常
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        return execute(invocation, invocation.getThis(), method, invocation.getArguments());
    }

    /**
     * 核心执行逻辑：完成方法调用并处理日志记录。
     *
     * @param invoker 方法调用上下文
     * @param target  目标对象
     * @param method  目标方法
     * @param args    方法参数
     * @return 方法的返回结果
     * @throws Throwable 执行过程中的异常
     */
    private Object execute(MethodInvocation invoker, Object target, Method method, Object[] args) throws Throwable {
        if (AopUtils.isAopProxy(target)) {
            return invoker.proceed();
        }

        Class<?> targetClass = getTargetClass(target);
        Object ret = null;
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(method, args, targetClass);
        OperationLogContext.putEmptySpan();
        Collection<OperationLogBuilder> operations = new ArrayList<>();
        Map<String, String> functionNameAndReturnMap = new HashMap<>();

        try {
            operations = operationLogSource.computeLogRecordOperations(method, targetClass);
        } catch (Exception e) {
            LogUtils.error("日志解析异常", e);
        }

        try {
            ret = invoker.proceed();
            methodExecuteResult.setResult(ret);
            methodExecuteResult.setSuccess(true);
        } catch (Exception e) {
            methodExecuteResult.setSuccess(false);
            methodExecuteResult.setThrowable(e);
            methodExecuteResult.setErrorMsg(e.getMessage());
        }

        processLogRecords(methodExecuteResult, functionNameAndReturnMap, operations);

        if (methodExecuteResult.getThrowable() != null) {
            throw methodExecuteResult.getThrowable();
        }

        return ret;
    }

    /**
     * 处理日志记录逻辑，根据方法执行结果生成操作日志。
     *
     * @param methodExecuteResult      方法执行结果
     * @param functionNameAndReturnMap 解析的函数名和返回值映射
     * @param operations               日志操作集合
     */
    private void processLogRecords(MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap,
                                   Collection<OperationLogBuilder> operations) {
        if (CollectionUtils.isEmpty(operations)) {
            return;
        }

        for (OperationLogBuilder operation : operations) {
            try {
                if (StringUtils.isAllEmpty(operation.getSuccessLogTemplate(), operation.getFailLogTemplate())) {
                    continue;
                }

                if (!methodExecuteResult.isSuccess()) {
                    handleFailureLog(methodExecuteResult, functionNameAndReturnMap, operation);
                } else {
                    handleSuccessLog(methodExecuteResult, functionNameAndReturnMap, operation);
                }
            } catch (Exception e) {
                LogUtils.error("日志执行异常", e);
            }
        }
    }

    /**
     * 处理成功的日志记录。
     *
     * @param methodExecuteResult      方法执行结果
     * @param functionNameAndReturnMap 函数名和返回值映射
     * @param operation                日志操作信息
     */
    private void handleSuccessLog(MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap,
                                  OperationLogBuilder operation) {
        String action = operation.getSuccessLogTemplate();
        if (StringUtils.isEmpty(action)) {
            return;
        }

        List<String> templates = getSpElTemplates(operation, action);
        String operatorId = resolveOperatorId(operation, templates);
        Map<String, String> expressionValues = processTemplate(templates, methodExecuteResult, functionNameAndReturnMap);

        saveLogRecord(methodExecuteResult.getMethod(), false, operation, operatorId, action, expressionValues);
    }

    /**
     * 处理失败的日志记录。
     *
     * @param methodExecuteResult      方法执行结果
     * @param functionNameAndReturnMap 函数名和返回值映射
     * @param operation                日志操作信息
     */
    private void handleFailureLog(MethodExecuteResult methodExecuteResult, Map<String, String> functionNameAndReturnMap,
                                  OperationLogBuilder operation) {
        if (StringUtils.isEmpty(operation.getFailLogTemplate())) {
            return;
        }

        String action = operation.getFailLogTemplate();
        List<String> templates = getSpElTemplates(operation, action);
        String operatorId = resolveOperatorId(operation, templates);
        Map<String, String> expressionValues = processTemplate(templates, methodExecuteResult, functionNameAndReturnMap);

        saveLogRecord(methodExecuteResult.getMethod(), true, operation, operatorId, action, expressionValues);
    }

    /**
     * 保存日志记录。
     *
     * @param method           方法对象
     * @param isFailure        是否为失败日志
     * @param operation        日志操作信息
     * @param operatorId       操作人 ID
     * @param action           操作描述
     * @param expressionValues 模板解析后的值
     */
    private void saveLogRecord(Method method, boolean isFailure, OperationLogBuilder operation, String operatorId,
                               String action, Map<String, String> expressionValues) {
        if (StringUtils.isEmpty(expressionValues.get(action))) {
            return;
        }

        OperationLog operationLog = OperationLog.builder()
                .type(expressionValues.get(operation.getType()))
                .resourceId(expressionValues.get(operation.getResourceId()))
                .operator(expressionValues.get(operatorId))
                .subType(expressionValues.get(operation.getSubType()))
                .extra(expressionValues.get(operation.getExtra()))
                .codeVariable(resolveCodeVariable(method))
                .action(expressionValues.get(action))
                .fail(isFailure)
                .createTime(new Date())
                .build();

        operationLogService.record(operationLog);
    }

    private Map<CodeVariableType, Object> resolveCodeVariable(Method method) {
        return Map.of(
                CodeVariableType.ClassName, method.getDeclaringClass(),
                CodeVariableType.MethodName, method.getName()
        );
    }

    private String resolveOperatorId(OperationLogBuilder operation, List<String> templates) {
        if (StringUtils.isEmpty(operation.getOperatorId())) {
            throw new IllegalArgumentException("[LogRecord] 操作人 ID 不能为空");
        }
        templates.add(operation.getOperatorId());
        return operation.getOperatorId();
    }

    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
    }

    private List<String> getSpElTemplates(OperationLogBuilder operation, String... actions) {
        List<String> spElTemplates = new ArrayList<>();
        spElTemplates.add(operation.getType());
        spElTemplates.add(operation.getResourceId());
        spElTemplates.add(operation.getSubType());
        spElTemplates.add(operation.getExtra());
        spElTemplates.addAll(Arrays.asList(actions));
        return spElTemplates;
    }

    @Override
    public void afterSingletonsInstantiated() {
        operationLogService = CommonBeanFactory.getBean(OperationLogService.class);
        this.setOperationLogFunctionParser(new OperationLogFunctionParser());
    }

}
