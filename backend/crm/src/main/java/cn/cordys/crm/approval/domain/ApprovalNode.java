package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_node")
public class ApprovalNode {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程ID")
    private String flowId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型：START/APPROVER/CONDITION/ELSE/END")
    private String nodeType;

    @Schema(description = "排序序号")
    private Integer sort;
}