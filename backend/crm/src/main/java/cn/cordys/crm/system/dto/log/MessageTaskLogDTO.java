package cn.cordys.crm.system.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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

  @Schema(description = "钉钉发送启用")
  private String dingTalkEnable;

  @Schema(description = "飞书发送启用")
  private String larkEnable;

  @Schema(description = "是否通知负责人")
  private String ownerEnable;

  @Schema(description = "负责人层级")
  private int ownerLevel;

  @Schema(description = "是否通知角色")
  private String roleEnable;

  @Schema(description = "通知人员ids,包含特殊值，负责人OWNER/创建人CREATE_USER")
  private List<String> userIdNames;

  @Schema(description = "通知角色ids")
  private List<String> roleIdNames;

  @Schema(description = "时间")
  private List<String> times;
}
