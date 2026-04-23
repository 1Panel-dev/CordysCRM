package cn.cordys.crm.approval.dto.response;

import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.ApproverConfigDTO;
import cn.cordys.crm.approval.dto.FieldPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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

    @Schema(description = "抄送人列表")
    private List<ApproverConfigDTO> cc;

    @Schema(description = "审批人列表")
    private List<ApproverConfigDTO> approver;

    @Schema(description = "审批通过后配置")
    private ApprovalPostConfigDTO passPostConfig;

    @Schema(description = "审批驳回后配置")
    private ApprovalPostConfigDTO rejectPostConfig;

    @Schema(description = "字段权限配置列表")
    private List<FieldPermissionDTO> fieldPermissions;
}