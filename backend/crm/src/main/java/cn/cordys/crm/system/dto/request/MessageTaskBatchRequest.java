package cn.cordys.crm.system.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageTaskBatchRequest {

    @Schema(description = "邮件启用")
    private Boolean emailEnable;

    @Schema(description = "系统启用")
    private Boolean sysEnable;

    @Schema(description = "企业微信启用")
    private Boolean weComEnable;

    @Schema(description = "钉钉启用")
    private Boolean dingTalkEnable;

    @Schema(description = "飞书启用")
    private Boolean larkEnable;

}
