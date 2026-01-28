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

  @Schema(description = "组织id")
  private String organizationId;

  @Schema(description = "消息模版")
  private byte[] template;
}
