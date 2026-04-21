package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.approval.constants.ApprovalTypeEnum;
import cn.cordys.crm.approval.constants.EmptyApproverActionEnum;
import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
import cn.cordys.crm.approval.constants.SameSubmitterActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批人节点请求")
public class ApprovalNodeApproverRequest extends ApprovalNodeRequest {

    @EnumValue(enumClass = ApprovalTypeEnum.class)
    @Schema(description = "审批类型：MANUAL/AUTO_PASS/AUTO_REJECT")
    private String approvalType;

    @EnumValue(enumClass = MultiApproverModeEnum.class)
    @Schema(description = "多人审批方式：ALL/ANY/SEQUENTIAL")
    private String multiApproverMode;

    @EnumValue(enumClass = EmptyApproverActionEnum.class)
    @Schema(description = "审批人为空时动作")
    private String emptyApproverAction;

    @EnumValue(enumClass = SameSubmitterActionEnum.class)
    @Schema(description = "审批人与提交人相同时动作")
    private String sameSubmitterAction;

    @Schema(description = "抄送人列表")
    private List<ApproverConfig> cc;

    @Schema(description = "审批人列表")
    private List<ApproverConfig> approver;

    @Schema(description = "审批通过后配置")
    private String passUpdateConfig;

    @Schema(description = "审批驳回后配置")
    private String rejectUpdateConfig;

    @Schema(description = "字段权限配置")
    private String fieldPermissions;

    @Data
    public static class ApproverConfig {
        @Schema(description = "审批人类型：MEMBER/SUPERIOR/DEPT_HEAD/ROLE/FORM_FIELD")
        private String type;
        @Schema(description = "审批人值")
        private String value;
    }
}