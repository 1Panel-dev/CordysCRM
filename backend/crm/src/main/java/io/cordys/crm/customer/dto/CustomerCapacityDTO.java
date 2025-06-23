package io.cordys.crm.customer.dto;

import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.system.dto.FilterConditionDTO;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomerCapacityDTO extends CustomerCapacity {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
	@Schema(description = "过滤条件集合")
	private List<FilterConditionDTO> filters;
}
