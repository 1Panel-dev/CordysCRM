package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CapacityAddRequest {

	@NotNull
	@Schema(description = "范围ID集合")
	private List<String> scopeIds;
	@Schema(description = "容量")
	private Integer capacity;

}
