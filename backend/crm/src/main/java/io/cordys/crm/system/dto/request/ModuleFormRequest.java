package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleFormRequest {

	@Size(max = 20)
	@NotBlank
	@Schema(description = "表单KEY", requiredMode = Schema.RequiredMode.REQUIRED)
	private String formKey;

	@Size(max = 32)
	@NotBlank
	@Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String organizationId;
}
