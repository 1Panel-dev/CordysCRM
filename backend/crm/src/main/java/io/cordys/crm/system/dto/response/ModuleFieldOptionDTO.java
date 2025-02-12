package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleFieldOptionDTO {

	@Size(max = 32)
	@NotBlank
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Size(max = 255)
	@NotBlank
	@Schema(description = "文本", requiredMode = Schema.RequiredMode.REQUIRED)
	private String label;
}
