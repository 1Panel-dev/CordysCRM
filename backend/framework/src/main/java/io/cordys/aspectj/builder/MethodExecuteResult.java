package io.cordys.aspectj.builder;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

@Getter
public class MethodExecuteResult {
    @Setter
    private boolean success;
    @Setter
    private Throwable throwable;
    @Setter
    private String errorMsg;

    @Setter
    private Object result;
    private final Method method;
    private final Object[] args;
    private final Class<?> targetClass;

    public MethodExecuteResult(Method method, Object[] args, Class<?> targetClass) {
        this.method = method;
        this.args = args;
        this.targetClass = targetClass;
    }

}
