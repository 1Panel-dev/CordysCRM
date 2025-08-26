package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClueTransformRequest {

	@NotBlank
	@Schema(description = "线索ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clueId;
	@Schema(description = "是否创建商机")
	private Boolean oppCreated;
	@Schema(description = "商机名称")
	private String oppName;
}
