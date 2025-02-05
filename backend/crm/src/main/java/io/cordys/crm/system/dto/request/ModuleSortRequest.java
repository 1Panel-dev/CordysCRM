package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModuleSortRequest {

	@Schema(description = "排序前", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long start;
	@Schema(description = "排序后", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long end;
	@Schema(description = "拖拽模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String dragModuleId;
}
