package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_node_approver")
public class ApprovalNodeApprover {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程ID")
    private String flowId;

    @Schema(description = "审批类型：MANUAL/AUTO_PASS/AUTO_REJECT")
    private String approvalType;

    @Schema(description = "多人审批方式：ALL/ANY/SEQUENTIAL")
    private String multiApproverMode;

    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction;

    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "抄送人（JSON数组）")
    private String cc;

    @Schema(description = "审批人（JSON数组）")
    private String approver;

    @Schema(description = "审批通过后配置（JSON格式）")
    private String passUpdateConfig;

    @Schema(description = "审批驳回后配置（JSON格式）")
    private String rejectUpdateConfig;

    @Schema(description = "字段权限配置（JSON格式）")
    private String fieldPermissions;
}