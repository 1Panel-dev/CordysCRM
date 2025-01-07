package io.cordys.crm.system.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleFieldRequest {

	@Size(min = 1, max = 32, message = "{module.id.length_range}", groups = {Created.class, Updated.class})
	@NotBlank(message = "{module.id.not_blank}", groups = {Created.class, Updated.class})
	@Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String moduleId;
}
