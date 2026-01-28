package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_message_task_config")
public class MessageTaskConfig {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "组织id")
  private String organizationId;

  @Schema(description = "通知事件类型")
  private String event;

  @Schema(description = "任务类型")
  private String taskType;

  @Schema(description = "通知配置值")
  private String value;
}
