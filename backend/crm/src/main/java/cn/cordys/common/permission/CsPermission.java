package cn.cordys.common.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义权限校验注解
 * 该注解可以替换 @RequiresPermissions，支持更复杂的权限校验逻辑，包括资源ID和审批状态权限。
 * 注：该注解不支持校验多个权限码，多个请使用 @RequiresPermissions 注解。
 * <p>
 * 四个判断依据：当前资源是你待办的资源 or (角色的权限位 && 角色的数据权限 && 审批流的状态权限)
 * <p>
 * 当 resourceId 为空时，仅校验角色权限位；
 * 指定 resourceId 时，若 approvalTaskId 不为空，则校验审批状态权限；否则校order验(角色的权限位 && 角色的数据权限 && 审批流的状态权限)。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CsPermission {

    /**
     * 权限码，如 "ORDER:READ"
     */
    String value();

    /**
     * 资源ID的SpEL表达式，如 "{#id}" 或 "{#request.id}"
     * 为空时仅校验角色权限位
     */
    String resourceId() default "";

    /**
     * 表单类型，如 "order"，用于审批状态权限校验
     * 为空时跳过审批状态权限校验
     */
    String formType() default "";
}
