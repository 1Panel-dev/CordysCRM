package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalResourceBaseParam {

	@NotBlank
	@Schema(description = "资源ID")
	private String resourceId;

	@NotBlank
	@Schema(description = "资源表单")
	private String formKey;
}
