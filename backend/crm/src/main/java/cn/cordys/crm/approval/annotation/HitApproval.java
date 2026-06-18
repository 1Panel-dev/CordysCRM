package cn.cordys.crm.approval.annotation;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用范围: 可配置审批流的业务模块执行方法上 (比如合同模块可配置审批流, 审批流执行时机支持创建, 编辑, 则在对应创建, 编辑业务方法上添加对应注解和参数即可)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HitApproval {

	/**
	 * 业务表单类型
	 */
	FormKey formKey();

	/**
	 * 执行时机
	 */
	ExecuteTimingEnum executeType();

	/**
	 * 资源ID表达式 (支持SpEL表达式，从返回值或者方法参数中获取资源ID)
	 */
	String resourceId() default "";

	/**
	 * TODO: 是否触发自动提审
	 */
	boolean autoSubmit() default false;

	/**
	 * 更新类型 (支持SpEL表达式，从返回值或者方法参数中获取资源ID) normal-正常更新  approval-评审更新
	 */
	String updateType() default "normal";

	/**
	 * 操作人 (审批流触发的用户ID)
	 */
	String operatorId();

	/**
	 * 变更说明
	 */
	String comment() default Strings.EMPTY;
}