package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleFieldDTO {

	@Schema(description = "ID")
	private String id;

	@Schema(description = "字段内置Key")
	private String internalKey;

	@Schema(description = "排序")
	private Long pos;

	@Schema(description = "字段属性")
	private String fieldProp;
}
