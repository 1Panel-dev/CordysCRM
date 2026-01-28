package cn.cordys.crm.integration.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MaxKBConfigDetailDTO {

  @Schema(description = "maxKB地址", requiredMode = Schema.RequiredMode.REQUIRED)
  private String mkAddress;

  @Schema(description = "apiKey")
  private String appSecret;

  @Schema(description = "是否验证通过")
  private Boolean verify;
}
