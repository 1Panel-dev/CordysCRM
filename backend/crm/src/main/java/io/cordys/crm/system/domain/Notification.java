package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Notification extends BaseModel {
    @Size(min = 1, max = 64, message = "{notification.type.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.type.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "通知类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
    @Size(min = 1, max = 50, message = "{notification.receiver.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.receiver.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "接收人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiver;
    @Size(min = 1, max = 255, message = "{notification.subject.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.subject.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;
    @Size(min = 1, max = 64, message = "{notification.status.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.status.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
    @Size(min = 1, max = 50, message = "{notification.operator.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.operator.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operator;
    @Size(min = 1, max = 50, message = "{notification.operation.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.operation.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "操作", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operation;
    @Size(min = 1, max = 64, message = "{notification.resource_type.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.resource_type.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "资源类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;
    @Size(min = 1, max = 255, message = "{notification.resource_name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{notification.resource_name.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceName;
    @NotBlank(message = "{notification.content.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
