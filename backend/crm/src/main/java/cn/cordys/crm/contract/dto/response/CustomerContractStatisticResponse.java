package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CustomerContractStatisticResponse {
  @Schema(description = "合同总金额")
  private BigDecimal totalAmount;
}
