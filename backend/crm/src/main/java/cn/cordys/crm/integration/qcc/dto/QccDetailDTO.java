package cn.cordys.crm.integration.qcc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class QccDetailDTO {

    @Schema(description = "企查查地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccAddress;

    @Schema(description = "是否验证通过")
    private Boolean verify;

    @Schema(description = "企查查AccessKey", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccAccessKey;

    @Schema(description = "企查查SecretKey", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccSecretKey;
}
