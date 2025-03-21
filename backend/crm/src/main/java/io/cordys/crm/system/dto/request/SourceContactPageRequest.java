package io.cordys.crm.system.dto.request;

import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class SourceContactPageRequest extends CustomerContactPageRequest {

	@NotBlank
	@Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String fieldId;
}
