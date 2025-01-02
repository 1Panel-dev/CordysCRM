package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_message_task_blob")
public class MessageTaskBlob{
    @Schema(description = "ID")
    private String id;
    @Schema(description = "消息模版")
    private byte[] template;
}
