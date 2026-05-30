package cn.cordys.aspectj.aop;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.annotation.OperationLogs;
import cn.cordys.aspectj.builder.OperationLogBuilder;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 该类负责解析和计算基于注解的日志记录操作。
 */
public class OperationLogSource {

    /**
     * 缓存方法与其对应接口方法的映射关系。
     */
    private static final Map<Method, Method> INTERFACE_METHOD_CACHE = new ConcurrentReferenceHashMap<>(256);

    /**
     * 确定给定方法对应的接口方法（如果可能）。
     *
     * @param method 要处理的方法
     *
     * @return 对应的接口方法，如果不存在则返回原方法
     */
    public static Method getInterfaceMethodIfPossible(Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
            return method;
        }
        return INTERFACE_METHOD_CACHE.computeIfAbsent(method, key -> {
            Class<?> current = key.getDeclaringClass();
            while (current != null && current != Object.class) {
                for (Class<?> ifc : current.getInterfaces()) {
                    try {
                        return ifc.getMethod(key.getName(), key.getParameterTypes());
                    } catch (NoSuchMethodException ex) {
                        // 忽略异常
                    }
                }
                current = current.getSuperclass();
            }
            return key;
        });
    }

    /**
     * 根据指定的方法和目标类计算日志记录操作。
     *
     * @param method      要分析的方法
     * @param targetClass 目标类
     *
     * @return 日志记录构建器的集合
     */
    public Collection<OperationLogBuilder> computeLogRecordOperations(Method method, Class<?> targetClass) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return Collections.emptyList();
        }

        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

        Collection<OperationLogBuilder> logRecordOps = parseLogRecordAnnotations(specificMethod);
        Collection<OperationLogBuilder> logRecordsOps = parseLogRecordsAnnotations(specificMethod);
        Collection<OperationLogBuilder> abstractLogRecordOps = parseLogRecordAnnotations(getInterfaceMethodIfPossible(method));
        Collection<OperationLogBuilder> abstractLogRecordsOps = parseLogRecordsAnnotations(getInterfaceMethodIfPossible(method));

        Set<OperationLogBuilder> result = new HashSet<>();
        result.addAll(logRecordOps);
        result.addAll(abstractLogRecordOps);
        result.addAll(logRecordsOps);
        result.addAll(abstractLogRecordsOps);
        return result;
    }

    /**
     * 解析方法上的 {@code LogRecords} 注解。
     *
     * @param ae 被注解的元素
     *
     * @return 日志记录构建器的集合
     */
    private Collection<OperationLogBuilder> parseLogRecordsAnnotations(AnnotatedElement ae) {
        return AnnotatedElementUtils.findAllMergedAnnotations(ae, OperationLogs.class).stream()
                .flatMap(operationLogs -> Arrays.stream(operationLogs.value())
                        .map(logRecord -> parseLogRecordAnnotation(ae, logRecord)))
                .collect(Collectors.toList());
    }

    /**
     * 解析方法上的 {@code LogRecord} 注解。
     *
     * @param ae 被注解的元素
     *
     * @return 日志记录构建器的集合
     */
    private Collection<OperationLogBuilder> parseLogRecordAnnotations(AnnotatedElement ae) {
        return AnnotatedElementUtils.findAllMergedAnnotations(ae, OperationLog.class).stream()
                .map(recordAnnotation -> parseLogRecordAnnotation(ae, recordAnnotation))
                .collect(Collectors.toList());
    }

    /**
     * 将 {@code LogRecord} 注解转换为日志记录构建器。
     *
     * @param ae               被注解的元素
     * @param recordAnnotation {@code LogRecord} 注解
     *
     * @return 日志记录构建器
     */
    private OperationLogBuilder parseLogRecordAnnotation(AnnotatedElement ae, OperationLog recordAnnotation) {
        return OperationLogBuilder.builder()
                .resourceName(recordAnnotation.resourceName())
                .type(recordAnnotation.type())
                .resourceId(recordAnnotation.resourceId())
                .operatorId(recordAnnotation.operator())
                .subType(recordAnnotation.module())
                .build();
    }

    /**
     * 验证日志记录操作配置是否合法。
     *
     * @param recordOps 日志记录构建器
     *
     * @throws IllegalStateException 如果配置不合法
     */
    public boolean isLogRecordOperationValidated(OperationLogBuilder recordOps) {
        LogContextInfo extra = OperationLogContext.getContext();
        String resourceName = recordOps.getResourceName();
        String resourceId = recordOps.getResourceId();
        if (extra != null && StringUtils.isNotBlank(extra.getResourceName())) {
            resourceName = extra.getResourceName();
        }
        if (extra != null && StringUtils.isNotBlank(extra.getResourceId())) {
            resourceId = extra.getResourceId();
        }

        return !StringUtils.isBlank(resourceName) && !StringUtils.isBlank(resourceId);
    }
}