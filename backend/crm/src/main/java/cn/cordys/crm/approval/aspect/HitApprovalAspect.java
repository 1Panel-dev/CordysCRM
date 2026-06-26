package cn.cordys.crm.approval.aspect;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.system.service.SysOperationLogService;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  切面后置操作: 当命中表单配置的审批流和执行时机时
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class HitApprovalAspect {

	private final ExpressionParser parser = new SpelExpressionParser();
	private final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

	/**
	 * 跳过审批检查的标记，用于审批通过后调用实际删除逻辑时避免再次触发审批
	 */
	private static final ThreadLocal<Boolean> SKIP_APPROVAL = new ThreadLocal<>();

	@Resource
	private ApprovalFlowService approvalFlowService;
	@Resource
	private ApprovalResourceService approvalResourceService;
	@Resource
	private SysOperationLogService sysOperationLogService;


	@Pointcut("@annotation(cn.cordys.crm.approval.annotation.HitApproval)")
	public void pointcut() {
	}

	@Around(value = "pointcut()")
	public Object handleHitApproval(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		HitApproval annotation = method.getAnnotation(HitApproval.class);

		if (annotation == null || Boolean.TRUE.equals(SKIP_APPROVAL.get())) {
			return joinPoint.proceed();
		}

		// DELETE 时机：先检查是否命中审批流，命中则不执行删除逻辑
		if (annotation.executeType() == ExecuteTimingEnum.DELETE) {
			return handleDeleteApproval(joinPoint, annotation, method);
		}

		// CREATE/UPDATE 时机：先执行方法，再检查审批
		Object retValue = joinPoint.proceed();

		try {
			String resourceId = resolveResourceId(method, joinPoint.getArgs(), annotation.resourceId(), retValue, annotation.executeType());
			String updateType = resolveResourceId(method, joinPoint.getArgs(), annotation.updateType(), retValue, annotation.executeType());
			String operator = resolveParamFromArgs(method, joinPoint.getArgs(), annotation.operatorId());
			String comment = resolveParamFromArgs(method, joinPoint.getArgs(), annotation.comment());
			if (StringUtils.isBlank(operator)) {
				operator = SessionUtils.getUserId();
			}
			if (StringUtils.isBlank(resourceId)) {
				return retValue;
			}

			if (Strings.CI.equals(updateType, "approval")) {
				return retValue;
			}

			// 获取组织ID
			String organizationId = OrganizationContext.getOrganizationId();
			if (StringUtils.isBlank(organizationId)) {
				return retValue;
			}

			ExecuteTimingEnum executeTiming = annotation.executeType();
			if (annotation.executeType() == ExecuteTimingEnum.UPDATE) {
				// UPDATE 时机：检查资源是否历史上审批通过过，如果没有审批通过过则视为CREATE时机
				boolean isCreateExecuteTime = !approvalResourceService.isResourceApproved(annotation.formKey(), resourceId);
				if (isCreateExecuteTime) {
					executeTiming = ExecuteTimingEnum.CREATE;
				}
			}

			// 检查是否命中审批流
			boolean hit = checkHitApprovalFlow(annotation.formKey(), executeTiming, organizationId);

			if (hit) {
				if (executeTiming == ExecuteTimingEnum.CREATE) {
					approvalResourceService.updateResourceApprovalStatus(annotation.formKey(), resourceId, ApprovalStatus.PENDING.name(), operator, OrganizationContext.getOrganizationId());
				} else {
					// 命中审批流，直接提审（跳过待提审状态）
					String updateFields = resolveUpdateFields();
					ApprovalPushParam pushParam = ApprovalPushParam.builder()
							.orgId(organizationId)
							.userId(operator)
							.resourceId(resourceId)
							.formKey(annotation.formKey().getKey())
							.executeTimingEnum(ExecuteTimingEnum.UPDATE)
							.updateFields(updateFields)
							.comment(comment)
							.build();
					approvalResourceService.push(pushParam);
				}
			}
		} catch (Exception e) {
			log.error("审批流执行时机匹配失败，error:{}", e.getMessage(), e);
		}

		return retValue;
	}

	/**
	 * 处理删除时机的审批逻辑：先检查审批，命中则不执行删除，未命中则正常删除
	 */
	private Object handleDeleteApproval(ProceedingJoinPoint joinPoint, HitApproval annotation, Method method) throws Throwable {
		// 从参数中解析资源ID（DELETE时资源ID从参数获取）
		String resourceId = resolveParamFromArgs(method, joinPoint.getArgs(), annotation.resourceId());
		String operator = resolveParamFromArgs(method, joinPoint.getArgs(), annotation.operatorId());

		if (StringUtils.isBlank(resourceId)) {
			return joinPoint.proceed();
		}

		if (StringUtils.isBlank(operator)) {
			operator = SessionUtils.getUserId();
		}

		// 获取组织ID
		String organizationId = OrganizationContext.getOrganizationId();
		if (StringUtils.isBlank(organizationId)) {
			return joinPoint.proceed();
		}

		// 检查是否命中审批流
		boolean hit = checkHitApprovalFlow(annotation.formKey(), annotation.executeType(), organizationId);

		if (!hit) {
			// 未命中审批流，直接执行删除
			return joinPoint.proceed();
		}

		ApprovalPushParam pushParam = ApprovalPushParam.builder()
				.orgId(organizationId)
				.userId(operator)
				.resourceId(resourceId)
				.formKey(annotation.formKey().getKey())
				.executeTimingEnum(ExecuteTimingEnum.DELETE)
				.comment(Translator.getWithArgs("approval.delete.resource", approvalResourceService.getFormKeyDisplayName(annotation.formKey()),
						approvalResourceService.getInstanceResourceName(annotation.formKey(), resourceId)))
				.build();
		approvalResourceService.push(pushParam);
		return null;
	}

	/**
	 * 解析修改的字段列表
	 */
	private String resolveUpdateFields() {
		try {
			LogContextInfo logContext = OperationLogContext.getContext();
			Object originalValue = logContext.getOriginalValue();
			Object modifiedValue = logContext.getModifiedValue();
			if (originalValue != null && modifiedValue != null) {
				List<JsonDifferenceDTO> jsonDifferences = sysOperationLogService.getJsonDifferences(JSON.toJSONString(originalValue), JSON.toJSONString(modifiedValue));
				List<String> fieldIds = jsonDifferences.stream()
						.map(JsonDifferenceDTO::getColumn)
						.map(col -> {
							if (col.contains("-")) {
								String[] split = col.split("-");
								return split[split.length - 1];
							}
							return col;
						})
						.collect(Collectors.toList());
				return JSON.toJSONString(fieldIds);
			}
		} catch (Exception e) {
			log.error("解析修改字段列表失败，error:{}", e.getMessage(), e);
		}
		return null;
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
		return resolveParamFromArgs(method, args, expression);
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
	private String resolveParamFromArgs(Method method, Object[] args, String expression) {
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
				case UPDATE -> Boolean.TRUE.equals(flow.getUpdateExecute());
				case DELETE -> Boolean.TRUE.equals(flow.getDeleteExecute());
			};
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 在跳过审批检查的情况下执行删除操作
	 * 用于DELETE审批通过后，执行实际的删除逻辑
	 *
	 * @param deleteAction 删除操作
	 */
	public static void executeDeleteSkipApproval(Runnable deleteAction) {
		SKIP_APPROVAL.set(true);
		try {
			deleteAction.run();
		} finally {
			SKIP_APPROVAL.remove();
		}
	}
}
