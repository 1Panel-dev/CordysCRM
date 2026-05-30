package cn.cordys.crm.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderStatisticResponse {

    @Schema(description = "总金额")
    private Double amount = 0.0;

    @Schema(description = "平均金额")
    private Double averageAmount = 0.0;
}
