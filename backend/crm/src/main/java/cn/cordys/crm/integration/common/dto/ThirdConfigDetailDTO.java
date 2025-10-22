package cn.cordys.crm.integration.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdConfigDetailDTO {
    @Schema(description = "企业ID")
    private String corpId;
    @Schema(description = "应用ID")
    private String agentId;
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "内部应用ID")
    private String appId;
    @Schema(description = "回调地址")
    private String redirectUrl;
    @Schema(description = "是否验证通过")
    private Boolean verify;
}
