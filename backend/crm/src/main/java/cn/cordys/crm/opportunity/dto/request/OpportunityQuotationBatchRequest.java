package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class OpportunityQuotationBatchRequest {

  @NotEmpty
  @Schema(description = "ID集合")
  private List<String> ids;

  @NotBlank
  @Schema(description = "审批状态")
  private String approvalStatus;
}
