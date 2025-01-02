package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageTask extends BaseModel {
    @Size(min = 1, max = 255, message = "{message_task.event.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{message_task.event.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "通知事件类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String event;

    @Size(min = 1, max = 1000, message = "{message_task.receivers.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{message_task.receivers.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "接收人id集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receivers;

    @Schema(description = "机器人id")
    private String projectRobotId;

    @Size(min = 1, max = 64, message = "{message_task.task_type.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{message_task.task_type.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @NotBlank(message = "{message_task.enable.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private int enable;

    @NotBlank(message = "{message_task.use_default_template.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "是否使用默认模版", requiredMode = Schema.RequiredMode.REQUIRED)
    private int useDefaultTemplate;

    @NotBlank(message = "{message_task.use_default_subject.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "是否使用默认标题（仅邮件）", requiredMode = Schema.RequiredMode.REQUIRED)
    private int useDefaultSubject;

    @Schema(description = "邮件标题")
    private String subject;
}
