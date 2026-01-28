package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContractStageRequest {

  @NotBlank
  @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @Size(max = 32)
  private String id;

  @Schema(
      description = "阶段: PENDING_SIGNING/SIGNED/IN_PROGRESS/COMPLETED_PERFORMANCE/VOID/ARCHIVED",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String stage;

  @Size(max = 255)
  @Schema(description = "作废原因")
  private String voidReason;
}
