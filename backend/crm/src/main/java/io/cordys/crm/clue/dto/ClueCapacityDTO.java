package io.cordys.crm.clue.dto;

import io.cordys.crm.clue.domain.ClueCapacity;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ClueCapacityDTO extends ClueCapacity {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
}
