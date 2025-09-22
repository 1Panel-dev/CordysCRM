package cn.cordys.crm.opportunity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OpportunitySearchStatisticResponse {

    @Schema(description = "总金额")
    private Double amount;

    @Schema(description = "平均金额")
    private Double averageAmount;
}
