package io.cordys.crm.system.dto;

import io.cordys.crm.system.domain.Module;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleDTO extends Module {

	@Schema(description = "是否禁用模块")
	private Boolean disabled;
}
