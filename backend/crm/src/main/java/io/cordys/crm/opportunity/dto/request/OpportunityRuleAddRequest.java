package io.cordys.crm.opportunity.dto.request;

import io.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpportunityRuleAddRequest {

	@NotBlank
	@Size(max = 255)
	@Schema(description = "规则名称")
	private String name;

	@NotNull
	@Schema(description = "范围ID集合")
	private List<String> scopeIds;

	@NotNull
	@Schema(description = "管理员ID集合")
	private List<String> ownerIds;

	@NotNull
	@Schema(description = "是否开启")
	private Boolean enable;

	@NotNull
	@Schema(description = "自动回收")
	private Boolean auto;

	@Size(max = 10)
	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "规则条件集合")
	private List<RuleConditionDTO> conditions;
}
