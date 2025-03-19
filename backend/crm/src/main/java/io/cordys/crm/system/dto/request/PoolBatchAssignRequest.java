package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoolBatchAssignRequest extends PoolBatchRequest {

	@NotBlank
	@Schema(description = "分配用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String assignUserId;
}
