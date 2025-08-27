package cn.cordys.mybatis.lambda;

import cn.cordys.common.util.LogUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Lambda 解析工具类。
 * <p>
 * 该工具类提供了从 Lambda 表达式中提取方法名称的功能，支持多种方式，包括代理、反射和序列化。
 * </p>
 */
public final class LambdaUtils {

    /**
     * 提取 Lambda 表达式的实现方法名称。
     * <p>
     * 根据不同的环境，方法会尝试通过代理、反射或序列化的方式来解析 Lambda 表达式。
     * </p>
     *
     * @param func 需要解析的 Lambda 对象。
     * @return 返回 Lambda 表达式的实现方法名称。
     */
    public static String extract(XFunction<?, ?> func) {
        // 1. IDEA 调试模式下，Lambda 表达式是一个代理对象
        if (func instanceof Proxy) {
            ProxyLambdaMeta lambdaMeta = new ProxyLambdaMeta((Proxy) func);
            return lambdaMeta.getImplMethodName();
        }

        // 2. 通过反射读取 Lambda 表达式中的元信息
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            ReflectLambdaMeta lambdaMeta = new ReflectLambdaMeta((java.lang.invoke.SerializedLambda) method.invoke(func), func.getClass().getClassLoader());
            return lambdaMeta.getImplMethodName();
        } catch (Throwable e) {
            // 3. 反射失败时，使用序列化方式读取 Lambda 元信息
            LogUtils.error("Extract lambda meta error", e);
            return new ShadowLambdaMeta(SerializedLambda.extract(func)).getImplMethodName();
        }
    }
}
