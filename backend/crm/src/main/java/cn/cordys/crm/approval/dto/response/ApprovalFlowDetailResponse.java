package cn.cordys.crm.approval.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalFlowDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程编码")
    private String number;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "表单类型")
    private String formType;

    @Schema(description = "执行时机")
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

    @Schema(description = "重复审批人规则")
    private String duplicateApproverRule;

    @Schema(description = "是否必须填写审批意见")
    private Boolean requireComment;

    @Schema(description = "对应资源表单的权限列表")
    private List<OptionDTO> permissions;

    @Schema(description = "状态权限配置")
    private List<StatusPermissionDTO> statusPermissions;

    @Schema(description = "节点配置列表")
    private List<ApprovalNodeResponse> nodes;
}