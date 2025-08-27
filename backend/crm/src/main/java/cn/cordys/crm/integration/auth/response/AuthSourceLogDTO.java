package cn.cordys.crm.integration.auth.response;

import cn.cordys.crm.integration.wecom.dto.WeComConfigurationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AuthSourceLogDTO {

    @Schema(description =  "认证源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description =  "描述")
    private String description;

    @Schema(description =  "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description =  "类型")
    private String type;

    @Schema(description =  "认证源配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private WeComConfigurationDTO configuration;

    @Schema(description =  "是否启用")
    private String enable;
}
