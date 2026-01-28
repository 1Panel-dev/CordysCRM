package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageTaskConfigRequest {
  @Schema(
      description = "消息配置模块: CUSTOMER/CLUE/BUSINESS/CONTRACT",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String module;

  @Schema(description = "消息配置事件", requiredMode = Schema.RequiredMode.REQUIRED)
  private String event;
}
