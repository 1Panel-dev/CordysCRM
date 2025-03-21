package io.cordys.crm.system.dto.request;

import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class SourceCustomerPageRequest extends CluePageRequest {

	@NotBlank
	@Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String fieldId;
}
