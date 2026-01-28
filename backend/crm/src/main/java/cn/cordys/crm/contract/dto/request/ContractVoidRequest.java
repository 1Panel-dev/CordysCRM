package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContractVoidRequest {

  @NotBlank
  @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Size(max = 255)
  @Schema(description = "作废原因")
  private String voidReason;
}
