package io.cordys.crm.customer.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class CustomerPoolSaveRequest {

	@NotBlank(groups = { Updated.class })
	@Size(max = 32, groups = { Created.class, Updated.class })
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "公海池名称")
	private String name;

	@NotBlank
	@Schema(description = "范围ID")
	private String scopeId;

	@NotBlank
	@Schema(description = "管理员ID")
	private String ownerId;

	@NonNull
	@Schema(description = "启用/禁用")
	private Boolean enable;

	@NonNull
	@Schema(description = "是否自动回收")
	private Boolean auto;

	@Schema(description = "领取规则")
	private CustomerPoolPickRuleSaveRequest pickRule;

	@Schema(description = "回收规则")
	private CustomerPoolRecycleRuleSaveRequest recycleRule;
}
