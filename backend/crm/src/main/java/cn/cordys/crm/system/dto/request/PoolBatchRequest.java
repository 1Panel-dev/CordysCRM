package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class PoolBatchRequest {

  @NotNull
  @Schema(description = "批量ID集合")
  private List<String> batchIds;
}
