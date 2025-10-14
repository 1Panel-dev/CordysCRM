package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageMoveRequest {

    @Schema(description = "放入位置序号pos", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long end;

    @Schema(description = "起始拖拽节点pos", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long start;

    @Schema(description = "拖拽节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dragId;
}
