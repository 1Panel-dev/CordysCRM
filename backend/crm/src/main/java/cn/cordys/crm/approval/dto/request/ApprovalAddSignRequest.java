package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ApprovalAddSignRequest extends ApprovalActionRequest {

    @Schema(description = "加签方式 BEFORE: 在我之前，AFTER: 在我之后")
    private String type;

	@NotEmpty
	@Schema(description = "加签审批人")
	private String signApprover;
}
