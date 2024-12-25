package io.cordys.mybatis.lambda;

/**
 * Lambda 信息
 */
public abstract class LambdaMeta {

    /**
     * 获取 lambda 表达式实现方法的名称
     *
     * @return lambda 表达式对应的实现方法名称
     */
    String getImplMethodName() {
        return "";
    }

    /**
     * 实例化该方法的类
     *
     * @return 返回对应的类名称
     */
    Class<?> getInstantiatedClass() {
        return null;
    }

    String toSnakeCase(String methodName) {
        String fieldName = methodName.replaceAll("get|set", "");
        // 使用正则将大写字母转换为小写并加上下划线
        String result = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2");
        return result.toLowerCase();
    }
}
