package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ReTransitionCustomerRequest {

	@NotBlank
	@Schema(description = "线索ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clueId;
	@NotBlank
	@Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String customerId;
}
