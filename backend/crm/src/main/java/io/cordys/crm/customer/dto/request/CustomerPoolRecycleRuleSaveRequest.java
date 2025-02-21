package io.cordys.crm.customer.dto.request;

import io.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerPoolRecycleRuleSaveRequest {

	@NotNull
	@Schema(description = "到期提醒")
	private Boolean expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;

	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "规则条件集合")
	private List<RuleConditionDTO> conditions;
}
