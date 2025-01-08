package io.cordys.crm.lead.dto;

import io.cordys.crm.lead.domain.LeadPool;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LeadPoolDTO extends LeadPool {

	@Schema(description = "管理员")
	private String ownerName;

	@Schema(description = "成员")
	private String scopeName;
}
