package cn.cordys.crm.approval.aspect;

import cn.cordys.common.constants.FormKey;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 *  切面后置操作: 当命中表单配置的审批流和执行时机时
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class HitApprovalAspect {

	private final ExpressionParser parser = new SpelExpressionParser();
	private final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

	@Resource
	private ApprovalFlowService approvalFlowService;
	@Resource
	private ApprovalResourceService approvalResourceService;

	@Pointcut("@annotation(cn.cordys.crm.approval.annotation.HitApproval)")
	public void pointcut() {
	}

	@AfterReturning(value = "pointcut()", returning = "retValue")
	public void handleHitApproval(JoinPoint joinPoint, Object retValue) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		HitApproval annotation = method.getAnnotation(HitApproval.class);

		if (annotation == null) {
			return;
		}

		try {
			String resourceId = resolveResourceId(method, joinPoint.getArgs(), annotation.resourceId(), retValue, annotation.executeType());
			String updateType = resolveResourceId(method, joinPoint.getArgs(), annotation.updateType(), retValue, annotation.executeType());
			if (StringUtils.isBlank(resourceId)) {
				return;
			}

			if (Strings.CI.equals(updateType, "approval")) {
				return;
			}

			// 获取组织ID
			String organizationId = OrganizationContext.getOrganizationId();
			if (StringUtils.isBlank(organizationId)) {
				return;
			}

			// 检查是否命中审批流
			boolean hit = checkHitApprovalFlow(annotation.formKey(), annotation.executeType(), organizationId);

			if (hit) {
				// 命中审批流, 修改业务资源审批状态为待提审
				approvalResourceService.clearResourceApprovalDetail(resourceId);
				approvalResourceService.updateResourceApprovalStatus(annotation.formKey(), resourceId, ApprovalStatus.PENDING.name());
			}
		} catch (Exception e) {
			log.error("审批流执行时机匹配失败，error:{}", e.getMessage(), e);
		}
	}

	/**
	 * 解析资源ID
	 * <p>
	 * 新增时从返回值获取，编辑时从参数获取
	 *
	 * @param method      方法
	 * @param args        方法参数
	 * @param expression  资源ID表达式
	 * @param retValue    方法返回值
	 * @param executeType 执行时机
	 * @return 资源ID
	 */
	private String resolveResourceId(Method method, Object[] args, String expression, Object retValue, ExecuteTimingEnum executeType) {
		if (executeType == ExecuteTimingEnum.CREATE) {
			// 新增从返回结果中获取资源ID
			return extractIdFromResult(retValue);
		}

		// 编辑从参数中解析
		return resolveResourceIdFromArgs(method, args, expression);
	}

	/**
	 * 从返回值中提取ID
	 */
	private String extractIdFromResult(Object retValue) {
		if (retValue == null) {
			return null;
		}
		// 如果返回值是字符串，直接返回
		if (retValue instanceof String) {
			return (String) retValue;
		}
		try {
			Method method = retValue.getClass().getMethod("getId");
			Object id = method.invoke(retValue);
			return id != null ? id.toString() : null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 从方法参数中解析资源ID（支持SpEL表达式）
	 */
	private String resolveResourceIdFromArgs(Method method, Object[] args, String expression) {
		String[] params = discoverer.getParameterNames(method);
		if (params == null || args == null) {
			return null;
		}

		EvaluationContext context = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			context.setVariable(params[i], args[i]);
		}

		try {
			Expression exp = parser.parseExpression(expression);
			Object value = exp.getValue(context, Object.class);
			if (value == null) {
				return null;
			}
			return value instanceof List ? ((List<?>) value).getFirst().toString() : value.toString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 检查是否命中审批流
	 *
	 * @param formKey         表单类型
	 * @param executeTiming   执行时机
	 * @param organizationId  组织ID
	 * @return 是否命中审批流
	 */
	private boolean checkHitApprovalFlow(FormKey formKey, ExecuteTimingEnum executeTiming, String organizationId) {
		try {
			// 查询当前组织表单审批流配置
			ApprovalFlow flow = approvalFlowService.getEnabledFlow(formKey.getKey(), organizationId);

			if (flow == null) {
				return false;
			}

			// 判断是否匹配执行时机
			return switch (executeTiming) {
				case CREATE -> Boolean.TRUE.equals(flow.getCreateExecute());
				case EDIT -> Boolean.TRUE.equals(flow.getUpdateExecute());
			};
		} catch (Exception e) {
			return false;
		}
	}
}
