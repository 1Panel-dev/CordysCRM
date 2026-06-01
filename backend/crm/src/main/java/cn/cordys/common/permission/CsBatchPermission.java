package cn.cordys.common.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量操作权限校验注解
 * <p>
 * 仅校验角色权限位和数据权限，不校验待办和审批状态权限
 * 状态权限业务代理里有校验，待办没有批量操作
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CsBatchPermission {

    /**
     * 权限码，如 "ORDER:UPDATE"
     */
    String value();

    /**
     * 资源ID的SpEL表达式，支持解析为单个ID或List<String>
     * 为空时仅校验角色权限位
     */
    String resourceId() default "";

    /**
     * 表单类型，如 "order"，用于数据权限校验
     */
    String formType() default "";
}
