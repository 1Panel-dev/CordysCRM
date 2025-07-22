package io.cordys.crm.system.dto.request;


import io.cordys.common.constants.EnumValue;
import io.cordys.crm.system.constants.DictType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DictAddRequest {

	@NotEmpty
	@Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@NotEmpty
	@Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED)
	@EnumValue(enumClass = DictType.class)
	private String type;
}
