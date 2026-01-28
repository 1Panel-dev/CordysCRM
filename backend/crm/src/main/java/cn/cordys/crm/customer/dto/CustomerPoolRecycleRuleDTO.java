package cn.cordys.crm.customer.dto;

import cn.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPoolRecycleRuleDTO {

  @Schema(description = "操作符")
  private String operator;

  @Schema(description = "规则条件集合")
  private List<RuleConditionDTO> conditions;
}
