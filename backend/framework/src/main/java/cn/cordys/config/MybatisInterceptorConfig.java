package cn.cordys.config;

import cn.cordys.common.util.EncryptUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Mybatis 拦截器配置类，主要用于配置 Mybatis 加密和解密拦截器。
 * <p>
 * 该类用于定义与加密/解密相关的拦截器设置，包括模型类、属性名、拦截器类及方法等。
 * </p>
 */
@Getter
@Setter
public class MybatisInterceptorConfig {

    /**
     * 模型类的名称。
     */
    private String modelName;

    /**
     * 需要拦截的属性名称。
     */
    private String attrName;

    /**
     * 需要拦截的属性名称列表。
     */
    private String attrNameForList;

    /**
     * 拦截器类的名称。
     */
    private String interceptorClass;

    /**
     * 拦截器的方法名。
     */
    private String interceptorMethod;

    /**
     * 反操作类（例如解密操作类）的名称。
     */
    private String undoClass;

    /**
     * 反操作方法名（例如解密方法）。
     */
    private String undoMethod;

    /**
     * 默认构造函数，初始化一个空的拦截器配置。
     */
    public MybatisInterceptorConfig() {
    }

    /**
     * 用于配置加密拦截器的构造函数。
     * <p>
     * 默认使用 {@link EncryptUtils} 类的加密和解密方法。
     * </p>
     *
     * @param modelClass 模型类
     * @param attrName   需要加密的属性名称
     */
    public MybatisInterceptorConfig(Class<?> modelClass, String attrName) {
        this.modelName = modelClass.getName();
        this.attrName = attrName;
        this.interceptorClass = EncryptUtils.class.getName();
        this.interceptorMethod = "aesEncrypt";
        this.undoClass = EncryptUtils.class.getName();
        this.undoMethod = "aesDecrypt";
    }

    /**
     * 用于自定义拦截器的构造函数。
     * <p>
     * 可以通过此构造函数传入自定义的拦截器类和方法。
     * </p>
     *
     * @param modelClass        模型类
     * @param attrName          需要拦截的属性名称
     * @param interceptorClass  自定义拦截器类
     * @param interceptorMethod 自定义拦截器方法
     * @param undoMethod        自定义反操作方法
     */
    public MybatisInterceptorConfig(Class<?> modelClass, String attrName, Class<?> interceptorClass, String interceptorMethod, String undoMethod) {
        this.modelName = modelClass.getName();
        this.attrName = attrName;
        this.interceptorClass = interceptorClass.getName();
        this.interceptorMethod = interceptorMethod;
        this.undoClass = interceptorClass.getName();
        this.undoMethod = undoMethod;
    }
}
