package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_node")
public class ApprovalNode {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "审批流版本ID")
    private String flowVersionId;

    @Schema(description = "节点编码")
    private String number;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型：START/APPROVER/CONDITION/DEFAULT/END")
    private String nodeType;

    @Schema(description = "排序序号")
    private Integer sort;

    @Schema(description = "执行时机：CREATE/UPDATE/DELETE")
    private String executeTime;
}
