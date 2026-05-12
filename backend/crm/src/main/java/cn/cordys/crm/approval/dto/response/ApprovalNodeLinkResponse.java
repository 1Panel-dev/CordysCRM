package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "审批节点连接响应")
public class ApprovalNodeLinkResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "源节点ID")
    private String fromNodeId;

    @Schema(description = "目标节点ID")
    private String toNodeId;
}
