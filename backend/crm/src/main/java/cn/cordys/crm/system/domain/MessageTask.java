package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_message_task")
public class MessageTask extends BaseModel {
    @Schema(description = "通知事件类型")
    private String event;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "系统启用")
    private Boolean sysEnable;

    @Schema(description = "类型(SYSTEM/CUSTOM)")
    private String type;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "消息模版")
    private String template;

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "状态")
    private Boolean enable;

    @Schema(description = "接收人")
    private String scopeId;

    @Schema(description = "接收方式")
    private String receiveMethod;
}
