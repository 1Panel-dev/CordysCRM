package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerPaymentPlanStatisticResponse {
    @Schema(description = "计划回款总金额")
    private BigDecimal totalPlanAmount;
}
