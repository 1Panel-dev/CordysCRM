package io.cordys.crm.lead.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
public class LeadPoolSaveRequest {

	@NotBlank(groups = { Updated.class })
	@Size(max = 32, groups = { Created.class, Updated.class })
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

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

	@NonNull
	@Schema(description = "启用/禁用")
	private Boolean enable;

	@NonNull
	@Schema(description = "自动回收")
	private Boolean auto;

	@Schema(description = "领取规则")
	private LeadPoolPickRuleSaveRequest pickRule;

	@Schema(description = "回收规则")
	private LeadPoolRecycleRuleSaveRequest recycleRule;
}
