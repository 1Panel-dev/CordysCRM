package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DictUpdateRequest {

	@NotEmpty
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@NotEmpty
	@Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;
}
