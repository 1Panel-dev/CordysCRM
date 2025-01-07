package io.cordys.common.permission;

import io.cordys.common.constants.InternalUser;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Set;

/**
 * @author jianxing
 */
public class PermissionUtils {
    public static boolean hasPermission(String permission) {
        SessionUser user = Objects.requireNonNull(SessionUtils.getUser());
        Set<String> permissionIds = user.getPermissionIds();
        if (StringUtils.equals(InternalUser.ADMIN.getValue(), user.getId())) {
            // admin 用户拥有所有权限
            return true;
        }
        // 判断是否拥有权限
        return permissionIds.contains(permission);
    }
}
