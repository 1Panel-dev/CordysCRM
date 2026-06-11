package cn.cordys.context;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Set;

/**
 * 组织上下文工具类，用于在请求/任务生命周期中传递组织ID和请求来源。
 * <p>
 * 使用 {@link InheritableThreadLocal} 实现父子线程自动传递，但在线程池场景需注意
 * 子线程任务执行完毕后必须清理，否则会造成上下文泄漏。
 * </p>
 *
 * <p>典型用法：</p>
 * <pre>
 *   OrganizationContext.setOrganizationId("123456");
 *   OrganizationContext.setRequestSource("API");
 *   try {
 *       // 业务逻辑
 *   } finally {
 *       OrganizationContext.clear();
 *   }
 * </pre>
 *
 * @author jianxing
 */
@Slf4j
public final class OrganizationContext {

    public static final String DEFAULT_ORGANIZATION_ID = "100001";

    private static final ThreadLocal<String> ORGANIZATION_ID = new InheritableThreadLocal<>();

    private OrganizationContext() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前请求/任务的组织ID，并进行权限校验。
     * <p>
     * 优先级：
     * <ol>
     *   <li>ThreadLocal 中已有值时，直接校验权限后返回</li>
     *   <li>无值时，根据用户信息自动推导（优先使用上次登录组织，否则任选一个）</li>
     *   <li>管理员且无组织权限时返回默认组织ID</li>
     * </ol>
     * 自动推导成功后会将结果存入 ThreadLocal，后续调用直接复用。
     * </p>
     *
     * @return 组织ID（可能为 {@code null}，仅当未登录且未手动设置时）
     *
     * @throws GenericException 如果当前用户对组织无权限
     */
    public static String getOrganizationId() {
        String orgId = ORGANIZATION_ID.get();
        SessionUser user = SessionUtils.getUser();
        if (user == null) {
            // 没有登入，则为定时任务，直接返回
            return orgId;
        }

        boolean isAdmin = Strings.CS.equals(InternalUser.ADMIN.getValue(), user.getId());

        if (StringUtils.isBlank(orgId)) {
            Set<String> organizationIds = user.getOrganizationIds();
            if (CollectionUtils.isNotEmpty(organizationIds)) {
                // 如果有组织权限
                if (organizationIds.contains(user.getLastOrganizationId())) {
                    // 如果上次登入的组织任有权限，则获取该组织ID
                    orgId = user.getLastOrganizationId();
                } else {
                    // 获取一个组织ID
                    orgId = organizationIds.iterator().next();
                }
                return orgId;
            } else if (isAdmin) {
                // 如果是管理员，则返回默认组织ID
                return DEFAULT_ORGANIZATION_ID;
            }
        } else if (user.getOrganizationIds().contains(orgId) || isAdmin) {
            // 如果用户有组织权限则返回
            return orgId;
        }

        // 没有权限，抛出异常
        throw new GenericException(CrmHttpResultCode.FORBIDDEN, "No organization permission");
    }

    /**
     * 手动设置组织ID（通常在过滤器或定时任务入口调用）。
     *
     * @param organizationId 组织ID，允许为 {@code null} 或空字符串
     */
    public static void setOrganizationId(String organizationId) {
        if (StringUtils.isNotBlank(organizationId)) {
            ORGANIZATION_ID.set(organizationId);
        }
    }

    /**
     * 清理 ThreadLocal 上下文，必须放在 finally 块中执行。
     */
    public static void clear() {
        ORGANIZATION_ID.remove();
    }
}