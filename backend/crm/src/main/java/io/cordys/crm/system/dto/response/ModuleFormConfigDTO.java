package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModuleFormConfigDTO {

	@Schema(description = "字段集合及其属性")
	private List<ModuleFieldDTO> fields;

	@Schema(description = "表单属性")
	private Map<String, Object> formProp;
}
