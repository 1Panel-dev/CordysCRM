package io.cordys.crm.system.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 组织成员
 * 
 * @author jianxing
 * @date 2025-01-08 17:00:38
 */
@Data
@Table(name = "sys_organization_user")
public class OrganizationUser extends BaseModel {

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "用户id")
	private String userId;

	@Schema(description = "部门id")
	private String departmentId;

	@Schema(description = "是否启用")
	private Boolean enable;

	@Schema(description = "工号")
	private String employeeId;

	@Schema(description = "职位")
	private String position;

	@Schema(description = "员工类型")
	private String employeeType;

	@Schema(description = "直属上级")
	private String supervisorId;

	@Schema(description = "工作城市")
	private String workCity;
}
