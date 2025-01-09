package io.cordys.crm.lead.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LeadCapacity extends BaseModel {

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "范围ID")
	private String scopeId;

	@Schema(description = "库容")
	private int capacity;
}
