package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-11-26 11:35
 */
@Data
public class CustomerContractPaymentPlanPageRequest extends ContractPaymentPlanPageRequest {
  @NotBlank
  @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String customerId;

  @Override
  public String getCustomerId() {
    return customerId;
  }
}
