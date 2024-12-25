package io.cordys.security;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.aop.PermissionAnnotationMethodInterceptor;

import java.lang.reflect.Parameter;

/**
 * 自定义权限注解方法拦截器，用于在权限验证前设置当前会话的组织标识。
 * <p>
 * 继承自 {@link PermissionAnnotationMethodInterceptor}，通过解析方法参数中的组织标识
 * （organizationId）来设置上下文环境，确保权限验证基于正确的组织上下文。
 * </p>
 *
 * <ul>
 *     <li>如果方法参数中包含名为 "organizationId" 的字符串参数，则设置其值到当前会话中。</li>
 *     <li>在权限验证完成后，清理会话中的组织标识。</li>
 * </ul>
 **/
public class CrmPermissionAnnotationMethodInterceptor extends PermissionAnnotationMethodInterceptor {

    /**
     * 构造函数，初始化注解解析器。
     *
     * @param resolver 注解解析器，用于解析方法上的权限注解。
     */
    public CrmPermissionAnnotationMethodInterceptor(AnnotationResolver resolver) {
        super(resolver);
    }

    /**
     * 权限验证方法。
     *
     * @param mi 方法调用上下文，包含目标方法及其参数信息。
     * @throws AuthorizationException 如果权限验证失败，抛出该异常。
     */
    @Override
    public void assertAuthorized(MethodInvocation mi) throws AuthorizationException {
        String organizationId = null;
        Object[] arguments = mi.getArguments();

        // 检查方法参数是否非空，并尝试获取组织标识
        if (ArrayUtils.isNotEmpty(arguments)) {
            Parameter[] parameters = mi.getMethod().getParameters();
            for (int i = 0; i < arguments.length; i++) {
                Object argument = arguments[i];
                if (argument instanceof String && "organizationId".equals(parameters[i].getName())) {
                    organizationId = (String) argument;
                    break;
                }
            }
        }

        // 设置当前会话的组织标识，并执行权限验证
        try {
            SessionUtils.setCurrentOrganizationId(organizationId);
            super.assertAuthorized(mi);
        } finally {
            // 清理当前会话的组织标识
            SessionUtils.clearCurrentOrganizationId();
        }
    }
}
