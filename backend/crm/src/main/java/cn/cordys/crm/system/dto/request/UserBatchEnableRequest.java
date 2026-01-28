package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserBatchEnableRequest extends UserBatchRequest {

  @Schema(description = "禁用/启用", requiredMode = Schema.RequiredMode.REQUIRED)
  boolean enable;
}
