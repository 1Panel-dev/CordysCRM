package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MaxKBThirdConfigRequest {

  @Schema(description = "api key", requiredMode = Schema.RequiredMode.REQUIRED)
  private String appSecret;

  @Schema(description = "maxKB地址", requiredMode = Schema.RequiredMode.REQUIRED)
  private String mkAddress;

  @Schema(description = "mk开启", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean mkEnable;
}
