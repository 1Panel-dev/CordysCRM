package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;

@Data
public class SysOperationLog extends BaseModel {

    @Size(min = 1, max = 32, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_operation_log.organization_id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "组织id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organizationId;

    @Size(min = 1, max = 32, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_operation_log.type.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "操作类型/add/update/delete", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Size(min = 1, max = 32, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_operation_log.module.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "操作模块", requiredMode = Schema.RequiredMode.REQUIRED)
    private String module;

    @Schema(description = "操作详情")
    private String details;

    @Size(min = 1, max = 32, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_operation_log.source_id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "资源id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceId;

    @Schema(description = "操作路径")
    private String path;

    @Size(min = 1, max = 255, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{sys_operation_log.method.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "操作方法", requiredMode = Schema.RequiredMode.REQUIRED)
    private String method;

    @Serial
    private static final long serialVersionUID = 1L;
}