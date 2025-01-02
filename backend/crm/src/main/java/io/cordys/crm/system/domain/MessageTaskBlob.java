package io.cordys.crm.system.domain;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageTaskBlob{
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{message_task_blob.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 32, message = "{message_task_blob.id.length_range}", groups = {Created.class, Updated.class})
    private String id;
    @Schema(description = "消息模版")
    private byte[] template;
}
