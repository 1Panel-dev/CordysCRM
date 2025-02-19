package io.cordys.crm.customer.dto.request;

import io.cordys.crm.system.dto.request.CapacityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
public class CustomerCapacitySaveRequest {

	@NonNull
	@Schema(description = "库容集合")
	List<CapacityRequest> capacities;
}
