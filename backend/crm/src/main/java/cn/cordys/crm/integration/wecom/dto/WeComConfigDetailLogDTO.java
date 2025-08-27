package cn.cordys.crm.integration.wecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WeComConfigDetailLogDTO {
    @Schema(description = "企业ID")
    private String corpId;
    @Schema(description = "应用ID")
    private String agentId;
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "扫码登录开启")
    private Boolean qrcodeEnable;
    @Schema(description = "同步开启")
    private Boolean syncEnable;
    @Schema(description = "同步开启")
    private Boolean weComEnable;
}
