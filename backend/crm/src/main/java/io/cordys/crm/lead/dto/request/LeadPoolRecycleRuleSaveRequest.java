package io.cordys.crm.lead.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

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

	@Schema(description = "回收条件")
	private String condition;
}
