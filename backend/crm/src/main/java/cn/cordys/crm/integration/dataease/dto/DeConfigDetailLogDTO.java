package cn.cordys.crm.integration.dataease.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeConfigDetailLogDTO {

    @Schema(description = "应用ID")
    private String deAppId;
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deAppSecret;
    @Schema(description = "回调地址")
    private String deUrl;
    @Schema(description = "DE仪表板开启")
    private Boolean deBoardEnable;
    @Schema(description = "DE自动同步")
    private Boolean deAutoSync;
    @Schema(description = "DEAccessKey")
    private String deAccessKey;
    @Schema(description = "DESecretKey")
    private String deSecretKey;
    @Schema(description = "DE组织id")
    private String deOrgID;
}
