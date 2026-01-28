package cn.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class BatchReTransitionCustomerRequest {

  @NotEmpty
  @Schema(description = "线索ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> clueIds;

  @NotBlank
  @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String customerId;
}
