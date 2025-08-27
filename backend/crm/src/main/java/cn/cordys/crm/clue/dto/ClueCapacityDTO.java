package cn.cordys.crm.clue.dto;

import cn.cordys.crm.clue.domain.ClueCapacity;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ClueCapacityDTO extends ClueCapacity {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
}
