package cn.cordys.crm.integration.sqlbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SqlBotConfigDetailDTO {

    @Schema(description = "嵌入脚本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "是否验证通过")
    private Boolean verify;
}
