package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_flow")
public class ApprovalFlow extends BaseModel {

    @Schema(description = "流程编码")
    private String number;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "表单类型：QUOTATION/CONTRACT/INVOICE/ORDER")
    private String formType;

    @Schema(description = "执行时机(JSON)")
    private String executeTiming;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "允许提交人撤销")
    private Boolean submitterCanRevoke;

    @Schema(description = "允许批量处理")
    private Boolean allowBatchProcess;

    @Schema(description = "允许撤回")
    private Boolean allowWithdraw;

    @Schema(description = "允许加签")
    private Boolean allowAddSign;

    @Schema(description = "重复审批人规则：FIRST_ONLY/SEQUENTIAL_ALL/EACH")
    private String duplicateApproverRule;

    @Schema(description = "是否必须填写审批意见")
    private Boolean requireComment;

    @Schema(description = "组织id")
    private String organizationId;
}