package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "审批节点连接请求")
public class ApprovalNodeLinkRequest {

    @Schema(description = "源节点ID")
    @NotBlank
    private String fromNodeId;

    @Schema(description = "目标节点ID")
    @NotBlank
    private String toNodeId;
}
