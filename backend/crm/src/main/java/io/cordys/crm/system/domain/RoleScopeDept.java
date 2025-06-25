package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 角色与部门的关联表，角色 data_scope 为指定部门时使用
 *
 * @author jianxing
 * @date 2025-01-23 18:18:10
 */
@Data
@Table(name = "sys_role_scope_dept")
public class RoleScopeDept {

	@Schema(description = "ID")
	private String id;

	@Schema(description = "角色ID")
	private String roleId;

	@Schema(description = "部门ID")
	private String departmentId;
}
