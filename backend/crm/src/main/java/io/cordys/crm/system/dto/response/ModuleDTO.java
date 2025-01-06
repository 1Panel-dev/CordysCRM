package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.Module;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleDTO extends Module {

	@Schema(description = "国际化名称")
	private String translateName;
}
