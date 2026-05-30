package cn.cordys.aspectj.builder;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

@Getter
public class MethodExecuteResult {
    private final Method method;
    private final Object[] args;
    private final Class<?> targetClass;
    @Setter
    private boolean success;
    @Setter
    private Throwable throwable;
    @Setter
    private String errorMsg;
    @Setter
    private Object result;

    public MethodExecuteResult(Method method, Object[] args, Class<?> targetClass) {
        this.method = method;
        this.args = args;
        this.targetClass = targetClass;
    }

}
