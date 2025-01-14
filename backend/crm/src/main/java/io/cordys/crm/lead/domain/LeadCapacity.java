package io.cordys.crm.lead.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "lead_capacity")
public class LeadCapacity extends BaseModel {

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "范围ID")
	private String scopeId;

	@Schema(description = "库容")
	private int capacity;
}
