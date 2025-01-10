package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;

public class CustomerPool extends BaseModel {

	@Schema(description = "范围ID")
	private String scopeId;

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "公海池名称")
	private String name;

	@Schema(description = "管理员ID")
	private String ownerId;

	@Schema(description = "启用/禁用")
	private int enable;

	@Schema(description = "是否自动回收")
	private int recycled;
}
