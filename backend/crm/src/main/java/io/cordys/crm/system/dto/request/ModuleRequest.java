package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleRequest {

	@Schema(description = "组织ID")
	private String organizationId;
}
