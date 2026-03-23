package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ContractStatisticResponse {

    @Schema(description = "总金额")
    private Double amount = 0.0;

    @Schema(description = "平均金额")
    private Double averageAmount = 0.0;
}
