package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpportunityStageRequest {

  @NotBlank
  @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @NotBlank
  @Schema(description = "商机阶段", requiredMode = Schema.RequiredMode.REQUIRED)
  private String stage;

  @Schema(description = "失败原因")
  private String failureReason;
}
