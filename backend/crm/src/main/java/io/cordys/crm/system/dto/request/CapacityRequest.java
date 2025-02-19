package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
public class CapacityRequest {

	@NonNull
	@Schema(description = "范围ID集合")
	private List<String> scopeIds;
	@Schema(description = "容量")
	private Integer capacity;

}
