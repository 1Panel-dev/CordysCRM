package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Table(name = "approval_node_approver")
public class ApprovalNodeApprover {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "审批流版本ID")
    private String flowVersionId;

    @NotBlank
    @Schema(description = "审批类型：MANUAL/AUTO_PASS/AUTO_REJECT")
    private String approvalType;

    @NotBlank
    @Schema(description = "多人审批方式：ALL/ANY/SEQUENTIAL")
    private String multiApproverMode;

    @NotBlank
    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction;

    @Schema(description = "审批人为空时，兜底审批人")
    private String fallbackApprover;

    @NotBlank
    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "审批人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String approverType;

    @Schema(description = "审批人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，适用于SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD")
    private String approverDirection;

    @Schema(description = "审批人列表（JSON数组）")
    private String approverList;

    @Schema(description = "抄送人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String ccType;

    @Schema(description = "抄送人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，适用于SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD")
    private String ccDirection;

    @Schema(description = "抄送人列表（JSON数组）")
    private String ccList;

    @Schema(description = "审批通过后配置（JSON格式）")
    private String passPostConfig;

    @Schema(description = "审批驳回后配置（JSON格式）")
    private String rejectPostConfig;

    @Schema(description = "字段权限配置（JSON格式）")
    private String fieldPermissions;
}
