package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class SourceProductPageRequest extends ProductPageRequest{

	@NotBlank
	@Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String fieldId;
}
