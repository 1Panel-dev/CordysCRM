package cn.cordys.crm.system.dto.request;


import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageTaskRequest {

    @Schema(description = "id")
    public String id;

    @Schema(description = "消息配置模块: CUSTOMER/CLUE/BUSINESS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{message_task.module.not_blank}", groups = {Created.class, Updated.class})
    public String module;

    @Schema(description = "消息配置事件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{message_task.event.not_blank}", groups = {Created.class, Updated.class})
    public String event;

    @Schema(description = "邮件启用")
    private boolean emailEnable;

    @Schema(description = "系统启用")
    private boolean sysEnable;

    @Schema(description = "企业微信启用")
    private boolean weComEnable;

    @Schema(description = "钉钉启用")
    private boolean dingTalkEnable;

    @Schema(description = "飞书启用")
    private boolean larkEnable;

}
