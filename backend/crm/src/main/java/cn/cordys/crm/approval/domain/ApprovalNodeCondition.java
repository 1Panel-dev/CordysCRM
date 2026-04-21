package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_node_condition")
public class ApprovalNodeCondition {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程ID")
    private String flowId;

    @Schema(description = "条件表达式JSON数组")
    private String ruleExpression;
}