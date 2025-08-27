package cn.cordys.crm.system.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageTaskLogDTO {

    @Schema(description = "通知事件类型")
    private String event;

    @Schema(description = "邮件发送开关")
    private String emailEnable;

    @Schema(description = "系统发送启用")
    private String sysEnable;

    @Schema(description = "企业微信发送启用")
    private String weComEnable;
}
