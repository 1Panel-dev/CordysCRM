package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoolCustomerBatchAssignRequest extends PoolCustomerBatchRequest {

	@NotBlank
	@Schema(description = "分配用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String assignUserId;
}
