package cn.cordys.crm.system.notice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendNotice {

    String module();

    /**
     * Event
     */
    String event() default "";

    /**
     * 消息主题
     */
    String subject() default "";

    /**
     * 获取实际值
     */
    String target() default "";

    /**
     * 资源目标
     */
    Class<?> targetClass() default Object.class;

    /**
     * 消息内容
     */
    String context() default "";

}
