package io.cordys.aspectj.annotation;

import java.lang.annotation.*;

/**
 * @author mr.zhao
 */
@Repeatable(OperationLogs.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OperationLog {
    /**
     * @return 日志的操作人
     * 没填，默认从 session 中获取
     */
    String operator() default "";

    /**
     * @return 操作日志的类型，如：新增、修改、删除
     * {@link io.cordys.aspectj.constants.LogType}
     */
    String type();

    /**
     * @return 业务模块名
     * {@link io.cordys.aspectj.constants.LogModule}
     */
    String module() default "";

    /**
     * @return 日志绑定的业务标识
     * 可以在注解中设置，也可以在 LogContextInfo 中设置
     * 优先级低于 LogContextInfo
     */
    String resourceId() default "";

    /**
     * @return 操作对象的名称
     * 可以在注解中设置，也可以在 LogContextInfo 中设置
     * 优先级低于 LogContextInfo
     */
    String resourceName() default "";
}
