package io.cordys.crm.integration.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdConfigurationDTO {
    @Schema(description = "类型")
    private String type;
    @Schema(description = "企业ID")
    private String corpId;
    @Schema(description = "应用ID")
    private String agentId;
    @Schema(description = "应用key")
    private String appKey;
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "扫码登录开启")
    private Boolean qrcodeEnable;
    @Schema(description = "同步开启")
    private Boolean syncEnable;
    @Schema(description = "同步开启")
    private Boolean weComEnable;
    @Schema(description = "是否验证通过")
    private Boolean verify;
    @Schema(description = "DE仪表板开启")
    private Boolean deBoardEnable;
    @Schema(description = "sqlBot仪表板开启")
    private Boolean sqlBotBoardEnable;
    @Schema(description = "sqlBot问数开启")
    private Boolean sqlBotChatEnable;
    @Schema(description = "DE账号")
    private String deAccount;
    @Schema(description = "回调地址")
    private String redirectUrl;
    @Schema(description = "DE模块嵌入")
    private Boolean deModuleEmbedding;
    @Schema(description = "DE外链集成")
    private Boolean deLinkIntegration;
    @Schema(description = "DE自动同步")
    private Boolean deAutoSync;
    @Schema(description = "DEAccessKey")
    private String deAccessKey;
    @Schema(description = "DESecretKey")
    private String deSecretKey;
    @Schema(description = "DE组织id")
    private String deOrgID;

}
