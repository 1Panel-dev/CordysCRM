package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_node_link")
public class ApprovalNodeLink {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程ID")
    private String flowId;

    @Schema(description = "源节点ID")
    private String fromNodeId;

    @Schema(description = "目标节点ID")
    private String toNodeId;

    @Schema(description = "分支评估顺序")
    private Integer sort;
}