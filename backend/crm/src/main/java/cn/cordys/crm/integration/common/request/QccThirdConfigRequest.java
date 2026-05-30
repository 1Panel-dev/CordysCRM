package cn.cordys.crm.integration.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class QccThirdConfigRequest {

    @Schema(description = "企查查开启", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean qccEnable;

    @Schema(description = "企查查地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccAddress;

    @Schema(description = "企查查AccessKey", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccAccessKey;

    @Schema(description = "企查查SecretKey", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qccSecretKey;
}
