package cn.cordys.context;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Set;

/**
 * 组织上下文
 * 定时任务手动，设置组织ID，调用结束后清理
 *
 * @author jianxing
 */
public class OrganizationContext {

    public static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final ThreadLocal<String> ORGANIZATION_ID = new InheritableThreadLocal<>();

    /**
     * 获取组织ID，并校验权限
     *
     * @return
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
     * 设置组织ID
     *
     * @param organizationId
     */
    public static void setOrganizationId(String organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
    }
}
