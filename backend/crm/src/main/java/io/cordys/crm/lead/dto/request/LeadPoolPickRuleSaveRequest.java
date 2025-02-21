package io.cordys.crm.lead.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeadPoolPickRuleSaveRequest {

	@NotNull
	@Schema(description = "是否限制领取数量")
	private Boolean limitOnNumber;

	@Schema(description = "领取数量")
	private Integer pickNumber;

	@NotNull
	@Schema(description = "是否限制前归属人领取")
	private Boolean limitPreOwner;

	@Schema(description = "领取间隔天数")
	private Integer pickIntervalDays;
}
