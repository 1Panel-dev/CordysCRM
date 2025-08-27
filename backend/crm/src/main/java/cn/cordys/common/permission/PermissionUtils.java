package cn.cordys.common.permission;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.context.OrganizationContext;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author jianxing
 */
public class PermissionUtils {
    public static boolean hasPermission(String permission) {
        PermissionCache permissionCache = CommonBeanFactory.getBean(PermissionCache.class);
        String userId = SessionUtils.getUserId();
        String organizationId = OrganizationContext.getOrganizationId();
        Set<String> permissionIds = permissionCache.getPermissionIds(userId, organizationId);
        SessionUser user = Objects.requireNonNull(SessionUtils.getUser());
        if (Strings.CS.equals(InternalUser.ADMIN.getValue(), user.getId())) {
            // admin 用户拥有所有权限
            return true;
        }
        // 判断是否拥有权限
        return permissionIds.contains(permission);
    }

    public static ResourceTabEnableDTO getTabEnableConfig(String userId, String permission, List<RolePermissionDTO> rolePermissions) {
        ResourceTabEnableDTO resourceTabEnableDTO = new ResourceTabEnableDTO();
        if (Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            resourceTabEnableDTO.setAll(true);
            resourceTabEnableDTO.setDept(true);
        }
        for (RolePermissionDTO rolePermission : rolePermissions) {
            if (!rolePermission.getPermissions().contains(permission)) {
                // 判断权限
                continue;
            }
            if (Strings.CS.equalsAny(rolePermission.getDataScope(), RoleDataScope.ALL.name(), RoleDataScope.DEPT_CUSTOM.name())) {
                // 数据权限为全部或指定部门，显示所有tab
                resourceTabEnableDTO.setAll(true);
            }
            if (Strings.CS.equalsAny(rolePermission.getDataScope(), RoleDataScope.ALL.name(), RoleDataScope.DEPT_CUSTOM.name(),
                    RoleDataScope.DEPT_AND_CHILD.name())) {
                // 数据权限为全部或部门，显示部门tab
                resourceTabEnableDTO.setDept(true);
            }
        }
        return resourceTabEnableDTO;
    }
}
