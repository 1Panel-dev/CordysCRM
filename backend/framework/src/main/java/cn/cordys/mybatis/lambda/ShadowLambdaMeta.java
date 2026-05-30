package cn.cordys.mybatis.lambda;

import jodd.util.StringPool;

/**
 * 基于 {@link SerializedLambda} 创建的元信息类。
 * <p>
 * 该类提供了获取 Lambda 实现方法名称和实例化类的功能。
 * 它扩展了 {@link LambdaMeta} 类，提供了对 Lambda 表达式的元数据的访问。
 * </p>
 */
public class ShadowLambdaMeta extends LambdaMeta {
    private final SerializedLambda lambda;

    /**
     * 构造方法，接收一个 {@link SerializedLambda} 对象。
     *
     * @param lambda {@link SerializedLambda} 对象，包含 Lambda 表达式的序列化信息。
     */
    public ShadowLambdaMeta(SerializedLambda lambda) {
        this.lambda = lambda;
    }

    /**
     * 获取 Lambda 实现的方法名称，并将其转换为蛇形命名法（snake_case）。
     *
     * @return 转换后的方法名称。
     */
    @Override
    public String getImplMethodName() {
        return toSnakeCase(lambda.getImplMethodName());
    }

    /**
     * 获取实例化类的 {@link Class} 对象。
     * 通过从 {@link SerializedLambda} 获取实例化类型的字符串，并将其转换为 {@link Class} 对象。
     *
     * @return 实例化类的 {@link Class} 对象。
     */
    @Override
    public Class<?> getInstantiatedClass() {
        String instantiatedMethodType = lambda.getInstantiatedMethodType();
        // 截取并转换类型名称
        String instantiatedType = instantiatedMethodType
                .substring(2, instantiatedMethodType.indexOf(StringPool.SEMICOLON))
                .replace(StringPool.SLASH, StringPool.DOT);
        // 使用类加载器加载实例化类
        return ClassUtils.toClassConfident(instantiatedType, lambda.getClass().getClassLoader());
    }
}
