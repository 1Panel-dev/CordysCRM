package io.cordys.crm.lead.dto.request;

import io.cordys.crm.system.dto.request.CapacityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LeadCapacitySaveRequest {

	@NotNull
	@Schema(description = "库容集合")
	List<CapacityRequest> capacities;
}
