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
}
