package io.cordys.crm.clue.dto.request;

import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ClueTransitionCustomerRequest extends CustomerAddRequest {

	@NotBlank
	@Schema(description = "线索ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clueId;
}
