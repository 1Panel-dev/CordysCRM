package io.cordys.crm.lead.dto.request;

import io.cordys.crm.lead.dto.LeadPoolPickRuleDTO;
import io.cordys.crm.lead.dto.LeadPoolRecycleRuleDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class LeadPoolAddRequest {

	@NotBlank
	@Size(max = 255)
	@Schema(description = "线索池名称")
	private String name;

	@NotNull
	@Schema(description = "范围ID集合")
	private List<String> scopeIds;

	@NotNull
	@Schema(description = "管理员ID集合")
	private List<String> ownerIds;

	@NotNull
	@Schema(description = "启用/禁用")
	private Boolean enable;

	@NotNull
	@Schema(description = "自动回收")
	private Boolean auto;

	@Schema(description = "领取规则")
	private LeadPoolPickRuleDTO pickRule;

	@Schema(description = "回收规则")
	private LeadPoolRecycleRuleDTO recycleRule;
}
