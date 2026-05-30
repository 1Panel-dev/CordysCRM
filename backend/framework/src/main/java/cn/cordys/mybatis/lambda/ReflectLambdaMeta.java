package cn.cordys.mybatis.lambda;

import jodd.util.StringPool;

import java.lang.invoke.SerializedLambda;

/**
 * 通过反射提供 Lambda 表达式元数据的类。
 * 该类提供了获取 Lambda 实现方法名称和实例化类的功能。
 */
public class ReflectLambdaMeta extends LambdaMeta {

    private final SerializedLambda lambda;
    private final ClassLoader classLoader;

    /**
     * 构造函数，初始化 ReflectLambdaMeta 实例。
     *
     * @param lambda      被序列化的 Lambda 表达式
     * @param classLoader 用于加载类的类加载器
     */
    public ReflectLambdaMeta(SerializedLambda lambda, ClassLoader classLoader) {
        this.lambda = lambda;
        this.classLoader = classLoader;
    }

    /**
     * 获取 Lambda 实现方法的名称，并转换为 snake_case 格式。
     *
     * @return Lambda 实现方法的名称（snake_case 格式）
     */
    @Override
    public String getImplMethodName() {
        return toSnakeCase(lambda.getImplMethodName());
    }

    /**
     * 获取 Lambda 表达式实例化的类。
     * 通过类加载器加载相应的类。
     *
     * @return Lambda 表达式实例化的类
     */
    @Override
    public Class<?> getInstantiatedClass() {
        String instantiatedMethodType = lambda.getInstantiatedMethodType();
        String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(StringPool.SEMICOLON)).replace(StringPool.SLASH, StringPool.DOT);
        return ClassUtils.toClassConfident(instantiatedType, this.classLoader);
    }

}
