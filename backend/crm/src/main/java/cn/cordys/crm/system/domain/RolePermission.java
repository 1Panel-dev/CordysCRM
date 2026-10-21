package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 角色权限
 *
 * @author jianxing
 */
@Data
@Table(name = "sys_role_permission")
public class RolePermission {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "角色id")
    private String roleId;

    @Schema(description = "权限id")
    private String permissionId;
}
