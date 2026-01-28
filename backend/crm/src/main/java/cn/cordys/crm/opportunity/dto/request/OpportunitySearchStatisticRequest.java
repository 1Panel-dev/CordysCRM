package cn.cordys.crm.opportunity.dto.request;

import cn.cordys.common.dto.condition.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OpportunitySearchStatisticRequest extends BaseCondition {
  @Schema(description = "客户ID")
  private String customerId;
}
