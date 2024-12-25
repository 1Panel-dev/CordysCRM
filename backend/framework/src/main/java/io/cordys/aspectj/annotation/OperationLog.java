package io.cordys.aspectj.annotation;

import java.lang.annotation.*;

@Repeatable(OperationLogs.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OperationLog {
    /**
     * @return 方法执行成功后的日志模版
     */
    String success();

    /**
     * @return 方法执行失败后的日志模版
     */
    String fail() default "";

    /**
     * @return 日志的操作人
     */
    String operator() default "";

    /**
     * @return 操作日志的类型，如：新增、修改、删除
     */
    String type();

    /**
     * @return 业务模块名
     */
    String module() default "";

    /**
     * @return 日志绑定的业务标识
     */
    String resourceId();

    /**
     * @return 日志的额外信息
     */
    String extra() default "";
}
