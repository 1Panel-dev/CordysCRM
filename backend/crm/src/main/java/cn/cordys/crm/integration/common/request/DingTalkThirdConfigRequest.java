package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DingTalkThirdConfigRequest {

    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String agentId;

    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpId;

    @Schema(description = "同步用户", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean startEnable;

    @Schema(description = "内部应用ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appId;
}
