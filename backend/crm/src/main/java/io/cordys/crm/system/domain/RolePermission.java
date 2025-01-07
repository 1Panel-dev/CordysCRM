package io.cordys.crm.system.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 角色权限
 * 
 * @author jianxing
 * @date 2025-01-03 17:58:31
 */
@Data
@Table(name = "sys_role_permission")
public class RolePermission extends BaseModel {

	@Schema(description = "角色id")
	private String roleId;

	@Schema(description = "权限id")
	private String permissionId;
}
