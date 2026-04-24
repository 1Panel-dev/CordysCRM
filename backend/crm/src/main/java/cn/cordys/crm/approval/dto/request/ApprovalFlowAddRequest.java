package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.approval.constants.DuplicateApproverRuleEnum;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalFlowAddRequest {

    @NotBlank(message = "流程名称不能为空")
    @Schema(description = "流程名称")
    private String name;

    @EnumValue(enumClass = ApprovalFormTypeEnum.class)
    @NotBlank(message = "表单类型不能为空")
    @Schema(description = "表单类型：quotation/contract/invoice/order")
    private String formType;

    @Schema(description = "执行时机列表：CREATE/EDIT")
    private List<String> executeTiming;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "允许提交人撤销")
    private Boolean submitterCanRevoke;

    @Schema(description = "允许批量处理")
    private Boolean allowBatchProcess;

    @Schema(description = "允许撤回")
    private Boolean allowWithdraw;

    @Schema(description = "允许加签")
    private Boolean allowAddSign;

    @EnumValue(enumClass = DuplicateApproverRuleEnum.class)
    @Schema(description = "重复审批人规则")
    private String duplicateApproverRule;

    @Schema(description = "是否必须填写审批意见")
    private Boolean requireComment;

    @Schema(description = "状态权限配置")
    private List<StatusPermissionDTO> statusPermissions;

    @Schema(description = "节点配置列表")
    private List<ApprovalNodeRequest> nodes;
}