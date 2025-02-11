package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SyncOrganizationDTO {
    @Schema(description = "类型")
    private String type;
    @Schema(description = "企业ID")
    private String corpId;
    @Schema(description = "应用ID")
    private String agentId;
    @Schema(description = "应用key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appKey;
    @Schema(description = "应用密钥")
    private String appSecret;
    @Schema(description = "id")
    private String id;
    @Schema(description = "是否开启")
    private Boolean enable;
    @Schema(description = "是否验证通过")
    private Boolean valid;

}
