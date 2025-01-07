package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ModuleFieldDTO extends ModuleField {

	@Schema(description = "字段选项值")
	private List<ModuleFieldOption> options;
}
