package cn.cordys.crm.system.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class MessageDetailDTO implements Serializable {
  private String id;
  private String event;
  private String taskType;
  private Long createTime;
  private String template;
  private String subject;
  private String organizationId;
  private boolean emailEnable;
  private boolean sysEnable;
  private boolean weComEnable;
  private boolean dingTalkEnable;
  private boolean larkEnable;
}
