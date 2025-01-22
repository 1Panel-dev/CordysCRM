package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProjectRobotConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "接收方式（站内信:IN_SITE, 邮件:MAIL）")
    private String receiveType;

    @Schema(description = "接收方是否开启")
    public Boolean enable;

    @Schema(description = "接收方是否使用默认模版")
    public Boolean useDefaultTemplate;

    @Schema(description = "接收方发送模版")
    public String template;

    @Schema(description = "接收方默认发送模版")
    public String defaultTemplate;

    @Schema(description = "邮件配置的标题")
    public String subject;

    @Schema(description = "接收方预览模版")
    public String previewTemplate;

}
