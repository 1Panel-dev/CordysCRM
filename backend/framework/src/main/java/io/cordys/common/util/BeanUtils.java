package io.cordys.common.util;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

/**
 * BeanUtils 提供了用于操作 Java Bean 的工具方法，包括属性复制、反射获取和设置属性值等。
 */
public class BeanUtils {

    /**
     * 复制源对象的属性到目标对象。
     *
     * @param target 目标对象
     * @param source 源对象
     * @param <T>    目标对象类型
     * @return 目标对象
     * @throws RuntimeException 如果复制过程失败，抛出运行时异常
     */
    public static <T> T copyBean(T target, Object source) {
        try {
            org.springframework.beans.BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object: ", e);
        }
    }

    /**
     * 复制源对象的属性到目标对象，并可以指定忽略的属性。
     *
     * @param target           目标对象
     * @param source           源对象
     * @param ignoreProperties 要忽略的属性名称
     * @param <T>              目标对象类型
     * @return 目标对象
     * @throws RuntimeException 如果复制过程失败，抛出运行时异常
     */
    public static <T> T copyBean(T target, Object source, String... ignoreProperties) {
        try {
            org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object: ", e);
        }
    }

    /**
     * 根据字段名获取 Java Bean 的属性值。
     *
     * @param fieldName 字段名称
     * @param bean      Java Bean 对象
     * @return 字段的值，如果获取失败则返回 null
     */
    public static Object getFieldValueByName(String fieldName, Object bean) {
        try {
            if (StringUtils.isBlank(fieldName)) {
                return null;
            }
            String getter = "get" + StringUtils.capitalize(fieldName);
            Method method = bean.getClass().getMethod(getter);
            return method.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据字段名和类型设置 Java Bean 的属性值。
     *
     * @param bean      Java Bean 对象
     * @param fieldName 字段名称
     * @param value     要设置的值
     * @param type      字段类型
     */
    public static void setFieldValueByName(Object bean, String fieldName, Object value, Class<?> type) {
        try {
            if (StringUtils.isBlank(fieldName)) {
                return;
            }
            String setter = "set" + StringUtils.capitalize(fieldName);
            Method method = bean.getClass().getMethod(setter, type);
            method.invoke(bean, value);
        } catch (Exception ignore) {
            // 可以根据需求记录日志或进行其他处理
        }
    }

    /**
     * 根据字段名和类型获取 Java Bean 的 setter 方法。
     *
     * @param bean      Java Bean 对象
     * @param fieldName 字段名称
     * @param type      字段类型
     * @return setter 方法，如果获取失败则返回 null
     */
    public static Method getMethod(Object bean, String fieldName, Class<?> type) {
        try {
            if (StringUtils.isBlank(fieldName)) {
                return null;
            }
            String setter = "set" + StringUtils.capitalize(fieldName);
            return bean.getClass().getMethod(setter, type);
        } catch (Exception e) {
            return null;
        }
    }
}
