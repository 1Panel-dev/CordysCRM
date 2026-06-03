package cn.cordys.crm.approval.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
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

    @Schema(description = "兜底审批人ID")
    private String fallbackApprover;

    @Schema(description = "兜底审批人名称")
    private String fallbackApproverName;

    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "审批人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String approverType;

    @Schema(description = "审批人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，适用于SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD")
    private String approverDirection;

    @Schema(description = "审批人ID列表")
    private List<String> approverList;

    @Schema(description = "审批人选择项（用于前端回显）")
    private List<OptionDTO> approverSelectOptions;

    @Schema(description = "抄送人类型：MEMBER/SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD/ROLE")
    private String ccType;

    @Schema(description = "抄送人方向：BOTTOM_UP(从下往上)/TOP_DOWN(从上往下)，适用于SUPERIOR/MULTIPLE_SUPERIOR/DEPT_HEAD/MULTIPLE_DEPT_HEAD")
    private String ccDirection;

    @Schema(description = "抄送人ID列表")
    private List<String> ccList;

    @Schema(description = "抄送人选择项（用于前端回显）")
    private List<OptionDTO> ccSelectOptions;

    @Schema(description = "审批通过后配置")
    private ApprovalPostConfigDTO passPostConfig;

    @Schema(description = "审批驳回后配置")
    private ApprovalPostConfigDTO rejectPostConfig;

    @Schema(description = "字段权限配置列表")
    private List<FieldPermissionDTO> fieldPermissions;
}
