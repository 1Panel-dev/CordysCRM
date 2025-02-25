package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleFieldRequest {

	@Schema(description = "表单Key")
	private String formKey;
	@Schema(description = "字段ID")
	private String fieldId;
}
