package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeThirdConfigRequest {

  @Schema(description = "APPID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String agentId;

  @Schema(description = "appSecret", requiredMode = Schema.RequiredMode.REQUIRED)
  private String appSecret;

  @Schema(description = "DEAccessKey", requiredMode = Schema.RequiredMode.REQUIRED)
  private String deAccessKey;

  @Schema(description = "DESecretKey", requiredMode = Schema.RequiredMode.REQUIRED)
  private String deSecretKey;

  @Schema(description = "回调地址", requiredMode = Schema.RequiredMode.REQUIRED)
  private String redirectUrl;

  @Schema(description = "DE组织id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String deOrgID;

  @Schema(description = "DE自动同步", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean deAutoSync;

  @Schema(description = "DE仪表板开启", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean deBoardEnable;
}
