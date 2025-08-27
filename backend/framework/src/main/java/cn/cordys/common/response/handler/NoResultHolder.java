package cn.cordys.common.response.handler;

import java.lang.annotation.*;

/**
 * 标记一个方法为“不需要返回结果”方法的注解。
 * <p>
 * 该注解用于标记那些不需要返回任何结果或对返回结果不做处理的方法。
 * 常用于控制器方法中，表明此方法调用后不需要返回任何数据给客户端。
 * </p>
 * <p>
 * 使用此注解的方法，可以避免框架或处理机制进行不必要的结果处理。
 * </p>
 *
 * @see java.lang.annotation.Annotation
 */
@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoResultHolder {
}
