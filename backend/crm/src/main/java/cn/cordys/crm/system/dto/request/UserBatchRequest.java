package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class UserBatchRequest {

  @Schema(description = "ID集合")
  private List<String> ids;
}
