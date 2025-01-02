package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_message_task")
public class MessageTask extends BaseModel {
    @Schema(description = "通知事件类型")
    private String event;

    @Schema(description = "接收人id集合")
    private String receivers;

    @Schema(description = "机器人id")
    private String projectRobotId;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "是否启用")
    private int enable;

    @Schema(description = "是否使用默认模版")
    private int useDefaultTemplate;

    @Schema(description = "是否使用默认标题（仅邮件）")
    private int useDefaultSubject;

    @Schema(description = "邮件标题")
    private String subject;
}
