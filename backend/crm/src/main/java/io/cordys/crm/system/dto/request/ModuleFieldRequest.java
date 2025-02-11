package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleFieldRequest {

	@Size(max = 32)
	@NotBlank
	@Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String moduleId;
}
