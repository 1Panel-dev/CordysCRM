package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SqlBotThirdConfigRequest {

    @Schema(description = "脚本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "sqlBot仪表板开启", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sqlBotBoardEnable;

    @Schema(description = "sqlBot问数开启", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sqlBotChatEnable;
}
