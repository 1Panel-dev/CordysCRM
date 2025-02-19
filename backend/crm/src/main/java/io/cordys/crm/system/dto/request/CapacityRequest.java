package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CapacityRequest {

	@Schema(description = "范围ID集合")
	private List<String> scopeIds;
	@Schema(description = "容量")
	private Integer capacity;

}
