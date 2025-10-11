package cn.cordys.crm.integration.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdConfigDetailLogDTO {
    @Schema(description = "企业ID")
    private String corpId;
    @Schema(description = "应用ID")
    private String agentId;
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "同步用户开启")
    private Boolean startEnable;
    @Schema(description = "回调地址")
    private String redirectUrl;
    @Schema(description = "oAuth2开启")
    private Boolean oAuth2Enable;
}
