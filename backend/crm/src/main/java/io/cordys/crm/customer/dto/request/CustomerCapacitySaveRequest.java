package io.cordys.crm.customer.dto.request;

import io.cordys.crm.system.dto.request.CapacityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomerCapacitySaveRequest {

	@Schema(description = "库容集合")
	List<CapacityRequest> capacities;
}
