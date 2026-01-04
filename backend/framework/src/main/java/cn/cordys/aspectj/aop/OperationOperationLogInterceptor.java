package cn.cordys.aspectj.aop;

import cn.cordys.aspectj.builder.MethodExecuteResult;
import cn.cordys.aspectj.builder.OperationLog;
import cn.cordys.aspectj.builder.OperationLogBuilder;
import cn.cordys.aspectj.builder.parse.OperationLogValueParser;
import cn.cordys.aspectj.constants.CodeVariableType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.handler.OperationLogService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.security.SessionUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

/**
 * 日志记录拦截器，拦截方法执行并生成日志记录。
 * <p>
 * 该类支持基于注解的日志模板解析，能够在方法执行的前后记录业务操作日志。
 * </p>
 */
@Slf4j
public class OperationOperationLogInterceptor extends OperationLogValueParser implements MethodInterceptor, Serializable, SmartInitializingSingleton {

    @Setter
    private OperationLogSource operationLogSource;

    private OperationLogService operationLogService;

    /**
     * 拦截方法执行，进行日志记录逻辑的处理。
     *
     * @param invocation 方法调用上下文
     *
     * @return 方法执行结果
     *
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
     *
     * @return 方法的返回结果
     *
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
        try {
            operations = operationLogSource.computeLogRecordOperations(method, targetClass);
        } catch (Exception e) {
            log.error("日志解析异常", e);
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

        processLogRecords(methodExecuteResult, operations);

        if (methodExecuteResult.getThrowable() != null) {
            throw methodExecuteResult.getThrowable();
        }

        return ret;
    }

    /**
     * 处理日志记录逻辑，根据方法执行结果生成操作日志。
     *
     * @param methodExecuteResult 方法执行结果
     * @param operations          日志操作集合
     */
    private void processLogRecords(MethodExecuteResult methodExecuteResult, Collection<OperationLogBuilder> operations) {
        if (CollectionUtils.isEmpty(operations)) {
            return;
        }

        for (OperationLogBuilder operation : operations) {
            try {
                if (!operationLogSource.isLogRecordOperationValidated(operation)) {
                    continue;
                }

                if (methodExecuteResult.isSuccess()) {
                    handleSuccessLog(methodExecuteResult, operation);
                }
            } catch (Exception e) {
                log.error("日志执行异常", e);
            }
        }
    }

    /**
     * 处理成功的日志记录。
     *
     * @param methodExecuteResult 方法执行结果
     * @param operation           日志操作信息
     */
    private void handleSuccessLog(MethodExecuteResult methodExecuteResult, OperationLogBuilder operation) {
        List<String> templates = getSpElTemplates(operation);
        String operatorId = resolveOperatorId(operation, templates);
        Map<String, String> expressionValues = processTemplate(templates, methodExecuteResult);

        saveLogRecord(methodExecuteResult.getMethod(), operation, operatorId, expressionValues);
    }

    /**
     * 保存日志记录。
     *
     * @param method           方法对象
     * @param operation        日志操作信息
     * @param operatorId       操作人 ID
     * @param expressionValues 模板解析后的值
     */
    private void saveLogRecord(Method method, OperationLogBuilder operation, String operatorId, Map<String, String> expressionValues) {
        OperationLog operationLog = OperationLog.builder()
                .type(expressionValues.get(operation.getType()))
                .resourceId(expressionValues.get(operation.getResourceId()))
                .resourceName(expressionValues.get(operation.getResourceName()))
                .operator(expressionValues.get(operatorId))
                .subType(expressionValues.get(operation.getSubType()))
                .codeVariable(resolveCodeVariable(method))
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
            // 如果没有标注操作人，则获取登入用户
            operation.setOperatorId(SessionUtils.getUserId());
        }
        templates.add(operation.getOperatorId());
        return operation.getOperatorId();
    }

    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
    }

    private List<String> getSpElTemplates(OperationLogBuilder operation) {
        List<String> spElTemplates = new ArrayList<>();
        spElTemplates.add(operation.getType());
        spElTemplates.add(operation.getResourceId());
        spElTemplates.add(operation.getResourceName());
        spElTemplates.add(operation.getSubType());
        return spElTemplates.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void afterSingletonsInstantiated() {
        operationLogService = CommonBeanFactory.getBean(OperationLogService.class);
    }

}
