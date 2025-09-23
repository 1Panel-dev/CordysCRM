package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class MessageTaskDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息配置事件")
    public String event;

    @Schema(description = "消息配置事件名称")
    public String eventName;

    @Schema(description = "邮件启用")
    private Boolean emailEnable;

    @Schema(description = "系统启用")
    private Boolean sysEnable;

    @Schema(description = "企业微信启用")
    private Boolean weComEnable;


}
