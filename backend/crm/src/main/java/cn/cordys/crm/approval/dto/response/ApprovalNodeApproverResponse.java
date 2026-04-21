package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批人节点响应")
public class ApprovalNodeApproverResponse extends ApprovalNodeResponse {

    @Schema(description = "审批类型")
    private String approvalType;

    @Schema(description = "审批类型名称")
    private String approvalTypeName;

    @Schema(description = "多人审批方式")
    private String multiApproverMode;

    @Schema(description = "多人审批方式名称")
    private String multiApproverModeName;

    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction;

    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "抄送人")
    private String cc;

    @Schema(description = "审批人")
    private String approver;

    @Schema(description = "审批通过后配置")
    private String passUpdateConfig;

    @Schema(description = "审批驳回后配置")
    private String rejectUpdateConfig;

    @Schema(description = "字段权限配置")
    private String fieldPermissions;
}