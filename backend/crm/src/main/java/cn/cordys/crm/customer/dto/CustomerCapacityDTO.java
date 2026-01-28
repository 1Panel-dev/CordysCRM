package cn.cordys.crm.customer.dto;

import cn.cordys.crm.customer.domain.CustomerCapacity;
import cn.cordys.crm.system.dto.FilterConditionDTO;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class CustomerCapacityDTO extends CustomerCapacity {

  @Schema(description = "成员集合")
  private List<ScopeNameDTO> members;

  @Schema(description = "过滤条件集合")
  private List<FilterConditionDTO> filters;
}
