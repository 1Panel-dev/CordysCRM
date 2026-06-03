package cn.cordys.common.permission;

import cn.cordys.context.OrganizationContext;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * @CsPermission 注解切面
 * <p>
 * 拦截标注了 @CsPermission 的方法，执行权限校验
 */
@Aspect
@Component
public class CsPermissionAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

    @Resource
    private ResourcePermissionService resourcePermissionService;


    @Before("@annotation(cn.cordys.common.permission.CsPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CsPermission annotation = method.getAnnotation(CsPermission.class);
        if (annotation == null) {
            return;
        }

        String permission = annotation.value();
        String resourceIdExpr = annotation.resourceId();

        if (resourceIdExpr.isEmpty()) {
            // 仅校验角色权限位
            resourcePermissionService.checkPermission(permission);
        } else {
            String formType = annotation.formType();
            String userId = SessionUtils.getUserId();
            String orgId = OrganizationContext.getOrganizationId();
            // 从方法参数中解析资源ID
            String resourceId = resolveResourceId(method, joinPoint.getArgs(), resourceIdExpr);
            // 校验 (1 && 2 && 3)
            resourcePermissionService.checkResourcePermission(permission, resourceId, formType, userId, orgId);
        }
    }

    /**
     * 从方法参数中解析资源ID（支持SpEL表达式）
     */
    private String resolveResourceId(Method method, Object[] args, String expression) {
        String[] params = discoverer.getParameterNames(method);
        if (params == null || args == null) {
            return null;
        }

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(params[i], args[i]);
        }

        try {
            var exp = parser.parseExpression(expression);
            Object value = exp.getValue(context, Object.class);
            if (value == null) {
                return null;
            }
            if (value instanceof List<?> list) {
                return list.isEmpty() ? null : list.getFirst().toString();
            }
            return value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量操作权限校验：仅校验角色权限位 + 数据权限，不校验待办和审批状态权限
     */
    @Before("@annotation(cn.cordys.common.permission.CsBatchPermission)")
    public void checkBatchPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CsBatchPermission annotation = method.getAnnotation(CsBatchPermission.class);
        if (annotation == null) {
            return;
        }

        String permission = annotation.value();
        String resourceIdExpr = annotation.resourceId();
        String formType = annotation.formType();
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();

        if (resourceIdExpr.isEmpty()) {
            resourcePermissionService.checkPermission(permission);
        } else {
            List<String> resourceIds = resolveResourceIds(method, joinPoint.getArgs(), resourceIdExpr);
            resourcePermissionService.checkBatchResourcePermission(permission, resourceIds, formType, userId, orgId);
        }
    }

    /**
     * 从方法参数中解析资源ID列表（支持SpEL表达式，结果为List<String>）
     * <p>
     * SpEL {expr} 会将结果包一层 list，如 {#request.ids} 中 ids 为 List 时，
     * 结果为 List<List<String>>，需要展平内层
     */
    private List<String> resolveResourceIds(Method method, Object[] args, String expression) {
        String[] params = discoverer.getParameterNames(method);
        if (params == null || args == null) {
            return List.of();
        }

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(params[i], args[i]);
        }

        try {
            var exp = parser.parseExpression(expression);
            Object value = exp.getValue(context, Object.class);
            if (value == null) {
                return List.of();
            }
            if (value instanceof List<?> list) {
                // SpEL {expr} 包了一层 list，如果唯一元素也是 list，则展平
                if (list.size() == 1 && list.getFirst() instanceof List<?> inner) {
                    return inner.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .toList();
                }
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            }
            return List.of(value.toString());
        } catch (Exception e) {
            return List.of();
        }
    }
}
