package cn.cordys.crm.approval.domain;

import cn.cordys.crm.approval.constants.EmptyApproverActionEnum;
import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Table(name = "approval_node_approver")
public class ApprovalNodeApprover {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程ID")
    private String flowId;

    @NotBlank
    @Schema(description = "审批类型：MANUAL/AUTO_PASS/AUTO_REJECT")
    private String approvalType = EmptyApproverActionEnum.AUTO_PASS.name();

    @NotBlank
    @Schema(description = "多人审批方式：ALL/ANY/SEQUENTIAL")
    private String multiApproverMode = MultiApproverModeEnum.ALL.name();

    @NotBlank
    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction = EmptyApproverActionEnum.AUTO_PASS.name();

    @NotBlank
    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "审批人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String approverType;

    @Schema(description = "审批人列表（JSON数组）")
    private String approverList;

    @Schema(description = "抄送人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String ccType;

    @Schema(description = "抄送人列表（JSON数组）")
    private String ccList;

    @Schema(description = "审批通过后配置（JSON格式）")
    private String passPostConfig;

    @Schema(description = "审批驳回后配置（JSON格式）")
    private String rejectPostConfig;

    @Schema(description = "字段权限配置（JSON格式）")
    private String fieldPermissions;
}