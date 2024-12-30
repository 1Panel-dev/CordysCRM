package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageTaskBlob extends BaseModel {
    @Schema(description = "消息模版")
    private String template;
}
