package io.cordys.crm.lead.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "lead_pool")
public class LeadPool extends BaseModel {

	@Schema(description = "线索池名称")
	private String name;

	@Schema(description = "成员ID")
	private String scopeId;

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "管理员ID")
	private String ownerId;

	@Schema(description = "启用/禁用")
	private Boolean enable;

	@Schema(description = "自动回收")
	private Boolean auto;
}