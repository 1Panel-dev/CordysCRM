package io.cordys.crm.integration.auth.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthSourceEditRequest {

    @Schema(description =  "认证源ID")
    @NotBlank(message = "{auth.id.not_blank}", groups = {Updated.class})
    private String id;

    @Schema(description =  "描述")
    private String description;

    @Schema(description =  "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{auth.name.not_blank}", groups = {Created.class, Updated.class})
    private String name;

    @Schema(description =  "类型")
    private String type;

    @Schema(description =  "认证源配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configuration;

    @Schema(description =  "是否启用")
    private Boolean enable;
}
