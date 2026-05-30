package cn.cordys.mybatis.lambda;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Executable;
import java.lang.reflect.Proxy;

/**
 * 在 IDEA 的 Evaluate 中执行的 Lambda 表达式元数据需要使用该类处理元数据
 */
public class ProxyLambdaMeta extends LambdaMeta {
    private final Class<?> clazz;
    private final String name;

    public ProxyLambdaMeta(Proxy func) {
        MethodHandle dmh = MethodHandleProxies.wrapperInstanceTarget(func);
        Executable executable = MethodHandles.reflectAs(Executable.class, dmh);
        clazz = executable.getDeclaringClass();
        name = executable.getName();
    }

    @Override
    public String getImplMethodName() {
        return toSnakeCase(name);
    }

    @Override
    public Class<?> getInstantiatedClass() {
        return clazz;
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + "::" + name;
    }

}