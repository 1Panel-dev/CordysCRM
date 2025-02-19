package io.cordys.crm.opportunity.dto;

import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityRuleDTO extends OpportunityRule {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
	@Schema(description = "管理员集合")
	private List<ScopeNameDTO> owners;
}
