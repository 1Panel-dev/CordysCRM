package io.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeOpportunityStatistic {

    @Schema(description = "跟进商机总数")
    private Long total;

    @Schema(description = "新增跟进商机")
    private HomeStatisticSearchResponse newOpportunity;

    @Schema(description = "跟进商机总额")
    private Long totalAmount;

}
