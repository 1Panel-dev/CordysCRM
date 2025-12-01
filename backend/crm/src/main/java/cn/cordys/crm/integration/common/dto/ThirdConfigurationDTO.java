package cn.cordys.crm.integration.common.dto;

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
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;
    @Schema(description = "内部应用ID")
    private String appId;
    @Schema(description = "同步用户")
    private Boolean startEnable;
    @Schema(description = "回调地址")
    private String redirectUrl;
    @Schema(description = "oAuth2开启")
    private Boolean oauth2Enable;
    @Schema(description = "是否验证通过")
    private Boolean verify;
    @Schema(description = "DE仪表板开启")
    private Boolean deBoardEnable;
    @Schema(description = "sqlBot仪表板开启")
    private Boolean sqlBotBoardEnable;
    @Schema(description = "sqlBot问数开启")
    private Boolean sqlBotChatEnable;
    @Schema(description = "DE自动同步")
    private Boolean deAutoSync;
    @Schema(description = "DEAccessKey")
    private String deAccessKey;
    @Schema(description = "DESecretKey")
    private String deSecretKey;
    @Schema(description = "DE组织id")
    private String deOrgID;
    @Schema(description = "maxKB地址")
    private String mkAddress;
    @Schema(description = "mk开启")
    private Boolean mkEnable;
    @Schema(description = "tender开启")
    private Boolean tenderEnable;

}
