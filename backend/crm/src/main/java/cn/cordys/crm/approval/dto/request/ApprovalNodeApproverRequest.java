package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.FieldPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批人节点请求")
public class ApprovalNodeApproverRequest extends ApprovalNodeRequest {

    @NotBlank
    @EnumValue(enumClass = ApprovalTypeEnum.class)
    @Schema(description = "审批类型：MANUAL/AUTO_PASS/AUTO_REJECT")
    private String approvalType = EmptyApproverActionEnum.AUTO_PASS.name();

    @NotBlank
    @EnumValue(enumClass = MultiApproverModeEnum.class)
    @Schema(description = "多人审批方式：ALL/ANY/SEQUENTIAL")
    private String multiApproverMode = MultiApproverModeEnum.ALL.name();

    @NotBlank
    @EnumValue(enumClass = EmptyApproverActionEnum.class)
    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction = EmptyApproverActionEnum.AUTO_PASS.name();

    @Schema(description = "审批人为空时，兜底审批人")
    private String fallbackApprover;

    @NotBlank
    @EnumValue(enumClass = SameSubmitterActionEnum.class)
    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction = SameSubmitterActionEnum.SKIP.name();

    @EnumValue(enumClass = ApproverTypeEnum.class)
    @Schema(description = "审批人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String approverType;

    @Schema(description = "审批人列表")
    private List<String> approverList;

    @EnumValue(enumClass = ApproverTypeEnum.class)
    @Schema(description = "抄送人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String ccType;

    @Schema(description = "抄送人列表")
    private List<String> ccList;

    @Schema(description = "审批通过后配置")
    private ApprovalPostConfigDTO passPostConfig;

    @Schema(description = "审批驳回后配置")
    private ApprovalPostConfigDTO rejectPostConfig;

    @Schema(description = "字段权限配置列表")
    private List<FieldPermissionDTO> fieldPermissions;
}