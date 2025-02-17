package io.cordys.crm.opportunity.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityRuleSaveRequest {

	@NotBlank(groups = { Updated.class })
	@Size(max = 32, groups = { Created.class, Updated.class })
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "规则名称")
	private String name;

	@NotBlank
	@Size(max = 1000)
	@Schema(description = "管理员ID")
	private String ownerId;

	@NotBlank
	@Size(max = 1000)
	@Schema(description = "范围ID")
	private String scopeId;

	@NonNull
	@Schema(description = "是否开启")
	private Boolean enable;

	@NonNull
	@Schema(description = "自动回收")
	private Boolean auto;

	@Size(max = 10)
	@Schema(description = "操作符")
	private String operator;

	@Schema(description = "回收条件")
	private String condition;

	@NonNull
	@Schema(description = "到期提醒")
	private Boolean expireNotice;

	@Schema(description = "提前提醒天数")
	private Integer noticeDays;
}
