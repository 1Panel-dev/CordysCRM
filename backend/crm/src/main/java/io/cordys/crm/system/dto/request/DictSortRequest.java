package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DictSortRequest {

	@NotEmpty
	@Schema(description = "字典ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String dragDictId;

	@NotEmpty
	@Schema(description = "排序前", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long start;

	@NotEmpty
	@Schema(description = "排序后", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long end;
}
