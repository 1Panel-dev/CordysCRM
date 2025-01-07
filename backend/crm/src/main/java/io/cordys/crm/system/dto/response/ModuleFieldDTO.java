package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.SysModuleField;
import io.cordys.crm.system.domain.SysModuleFieldOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ModuleFieldDTO extends SysModuleField {

	@Schema(description = "字段选项值")
	private List<SysModuleFieldOption> options;
}
