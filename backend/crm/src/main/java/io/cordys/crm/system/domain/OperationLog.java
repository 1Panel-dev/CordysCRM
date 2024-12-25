package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OperationLog extends BaseModel {

    @Schema(description = "组织id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.organization_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{operation_log.organization_id.length_range}", groups = {Created.class, Updated.class})
    private String organizationId;

    @Schema(description = "资源id")
    private String sourceId;

    @Schema(description = "操作方法", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.method.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    private String method;

    @Schema(description = "操作类型/add/update/delete", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{operation_log.type.length_range}", groups = {Created.class, Updated.class})
    private String type;

    @Schema(description = "操作模块/api/case/scenario/ui")
    private String module;

    @Schema(description = "操作详情")
    private String content;

    @Schema(description = "操作路径")
    private String path;

    @Serial
    private static final long serialVersionUID = 1L;
}