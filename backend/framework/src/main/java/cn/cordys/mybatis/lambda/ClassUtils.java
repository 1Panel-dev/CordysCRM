package cn.cordys.mybatis.lambda;

import cn.cordys.common.exception.GenericException;
import org.apache.ibatis.io.Resources;

import java.util.Arrays;
import java.util.List;

/**
 * 用于操作类的工具类，提供检查类是否为代理类以及通过不同的类加载器加载类的方法。
 */
public final class ClassUtils {

    /**
     * 代理类名称的列表。
     */
    private static final List<String> PROXY_CLASS_NAMES = Arrays.asList(
            "net.sf.cglib.proxy.Factory", // cglib
            "org.springframework.cglib.proxy.Factory", // cglib
            "javassist.util.proxy.ProxyObject", // javassist
            "org.apache.ibatis.javassist.util.proxy.ProxyObject" // javassist
    );
    private static ClassLoader systemClassLoader;

    static {
        try {
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (SecurityException ignored) {
            // Google App Engine 中的 AccessControlException 异常
        }
    }

    // 私有构造函数，防止实例化
    private ClassUtils() {
    }

    /**
     * 判断传入的类型是否是布尔类型（包括原生布尔和包装类型布尔）。
     *
     * @param type 要检查的类类型。
     *
     * @return 如果是布尔类型或其包装类型，则返回 {@code true}，否则返回 {@code false}。
     */
    public static boolean isBoolean(Class<?> type) {
        return type == boolean.class || Boolean.class == type;
    }

    /**
     * 判断传入的类是否为代理类。
     *
     * @param clazz 要检查的类。
     *
     * @return 如果类是代理类，返回 {@code true}，否则返回 {@code false}。
     */
    public static boolean isProxy(Class<?> clazz) {
        if (clazz != null) {
            for (Class<?> cls : clazz.getInterfaces()) {
                if (PROXY_CLASS_NAMES.contains(cls.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 使用指定的类名和类加载器加载类。该方法确保类是从有效的类加载器中加载的。
     *
     * @param name        类的全限定名。
     * @param classLoader 类加载器。
     *
     * @return 与类名对应的类对象。
     *
     * @throws GenericException 如果类无法找到或加载。
     */
    public static Class<?> toClassConfident(String name, ClassLoader classLoader) {
        try {
            return loadClass(name, getClassLoaders(classLoader));
        } catch (ClassNotFoundException e) {
            throw new GenericException("找不到指定的class！请仅在明确确定会有class的时候，调用该方法", e);
        }
    }

    /**
     * 尝试通过多个类加载器加载类。
     *
     * @param className    类的全限定名。
     * @param classLoaders 类加载器数组。
     *
     * @return 与类名对应的类对象。
     *
     * @throws ClassNotFoundException 如果无法找到类。
     */
    private static Class<?> loadClass(String className, ClassLoader[] classLoaders) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null) {
                try {
                    return Class.forName(className, true, classLoader);
                } catch (ClassNotFoundException e) {
                    // 忽略异常，尝试下一个类加载器
                }
            }
        }
        throw new ClassNotFoundException("无法找到类: " + className);
    }

    /**
     * 获取用于加载类的类加载器数组。
     *
     * @param classLoader 主类加载器。
     *
     * @return 用于加载类的类加载器数组。
     */
    private static ClassLoader[] getClassLoaders(ClassLoader classLoader) {
        return new ClassLoader[]{
                classLoader,
                Resources.getDefaultClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassUtils.class.getClassLoader(),
                systemClassLoader
        };
    }
}
