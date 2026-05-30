package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerContractStatisticResponse {
    @Schema(description = "合同总金额")
    private BigDecimal totalAmount;
}
