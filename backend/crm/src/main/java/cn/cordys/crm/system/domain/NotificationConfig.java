package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_notification_config")
public class NotificationConfig {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "通知类型")
    private String type;

    @Schema(description = "通知配置值")
    private String value;
}
