package io.cordys.crm.lead.dto.request;

import io.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class LeadPoolRecycleRuleSaveRequest {

	@NonNull
	@Schema(description = "到期提醒")
	private Boolean expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;

	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "规则条件集合")
	private List<RuleConditionDTO> conditions;
}
