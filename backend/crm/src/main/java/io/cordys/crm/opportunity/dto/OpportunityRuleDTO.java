package io.cordys.crm.opportunity.dto;

import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityRuleDTO extends OpportunityRule {

	@Schema(description = "成员范围")
	private List<String> scopeNames;
	@Schema(description = "管理员集合")
	private List<String> ownerNames;
}
