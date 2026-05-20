package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalAddSignRequest extends ApprovalActionRequest {

    @Schema(description = "加签方式 BEFORE: 在我之前，AFTER: 在我之后")
    private String type;

	@Schema(description = "加签审批人")
	private String signApprover;
}
