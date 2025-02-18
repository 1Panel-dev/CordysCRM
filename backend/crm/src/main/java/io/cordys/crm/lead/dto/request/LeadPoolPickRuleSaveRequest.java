package io.cordys.crm.lead.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class LeadPoolPickRuleSaveRequest {

	@NonNull
	@Schema(description = "是否限制领取数量")
	private Boolean limitOnNumber;

	@Schema(description = "领取数量")
	private Integer pickNumber;

	@NonNull
	@Schema(description = "是否限制前归属人领取")
	private Boolean limitPreOwner;

	@Schema(description = "领取间隔天数")
	private Integer pickIntervalDays;
}
