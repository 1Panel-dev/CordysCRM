package io.cordys.crm.lead.dto;

import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class LeadCapacityDTO extends LeadCapacity {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
}
