package io.cordys.crm.integration.sqlbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SqlBotConfigDetailLogDTO {

    @Schema(description = "嵌入脚本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sqlBotAppSecret;

    @Schema(description = "sqlBot仪表板开启")
    private Boolean sqlBotBoardEnable;

    @Schema(description = "sqlBot问数开启")
    private Boolean sqlBotChatEnable;
}
