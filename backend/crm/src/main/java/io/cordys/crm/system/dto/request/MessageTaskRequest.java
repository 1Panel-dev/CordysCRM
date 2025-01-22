package io.cordys.crm.system.dto.request;


import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MessageTaskRequest {

    @Schema(description = "消息配置模块", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{message_task.module.not_blank}", groups = {Created.class, Updated.class})
    public String module;

    @Schema(description = "消息配置场景", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{message_task.event.not_blank}", groups = {Created.class, Updated.class})
    public String event;

    @Schema(description = "消息配置接收人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{message_task.receivers.not_empty}", groups = {Created.class, Updated.class})
    private List<String> receiverIds;

    @Schema(description = "具体测试的ID")
    public String testId;

    @Schema(description = "消息配置接收方式")
    public String receiveType;

    @Schema(description = "消息配置是否开启")
    public Boolean enable;

    @Schema(description = "消息配置企业用户自定义的消息模版")
    public String template;

    @Schema(description = "是否使用默认模版")
    private Boolean useDefaultTemplate;



}
