package io.cordys.crm.clue.dto;

import io.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CluePoolRecycleRuleDTO {

	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "规则条件集合")
	private List<RuleConditionDTO> conditions;
}
