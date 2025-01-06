package io.cordys.crm.system.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleRequest {

	@Size(min = 1, max = 32, message = "{announcement.organizationId.length_range}", groups = {Created.class, Updated.class})
	@NotBlank(message = "{announcement.organizationId.not_blank}", groups = {Created.class, Updated.class})
	@Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String organizationId;
}
