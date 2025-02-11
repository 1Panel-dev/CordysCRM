package io.cordys.common.permission;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 加载的权限定义
 * @author jianxing
 */
@Data
public class PermissionDefinition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<RoleResource> resource;
    private List<Permission> permissions;
}
