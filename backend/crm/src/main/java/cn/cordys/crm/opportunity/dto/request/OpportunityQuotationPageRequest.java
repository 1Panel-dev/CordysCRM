package cn.cordys.crm.opportunity.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author guoyuqi
 */
@Data
public class OpportunityQuotationPageRequest extends BasePageRequest {

  /** 商机id */
  @Schema(description = "商机id")
  private String opportunityId;
}
