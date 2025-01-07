package io.cordys.crm.system.dto.request;

import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ModuleFieldSaveRequest extends ModuleFieldRequest{

	@Schema(description = "保存字段集合")
	private List<ModuleFieldDTO> fields;

	@Schema(description = "删除字段ID集合")
	private List<String> deleteFieldIds;
}
