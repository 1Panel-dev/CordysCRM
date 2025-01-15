package io.cordys.crm.customer.dto.request;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCapacitySaveRequest extends BaseModel {

	@NotBlank(message = "{scope_id.not_blank}")
	@Size(min = 1, max = 1000, message = "{scope_id.length_range}")
	@Schema(description = "范围ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String scopeId;

	@Schema(description = "库容", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
	private int capacity;
}
