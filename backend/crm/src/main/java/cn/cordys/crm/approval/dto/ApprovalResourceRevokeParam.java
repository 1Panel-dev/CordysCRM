package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalResourceRevokeParam extends ApprovalResourceBaseParam {

	@NotBlank
	@Schema(description = "审批实例ID")
	private String instanceId;
}
