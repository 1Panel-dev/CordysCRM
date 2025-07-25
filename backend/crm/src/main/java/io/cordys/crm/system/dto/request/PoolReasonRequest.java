package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PoolReasonRequest {

	@NotEmpty
	@Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "原因")
	private String reasonId;
}
