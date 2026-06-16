package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "approval_flow")
public class ApprovalFlow extends BaseModel {

    @Schema(description = "当前版本ID")
    private String currentVersionId;

    @Schema(description = "流程编码")
    private String number;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "表单类型：quotation/contract/invoice/order")
    private String formType;

    @Schema(description = "新建时执行")
    private Boolean createExecute;

    @Schema(description = "编辑时执行")
    private Boolean updateExecute;

    @Schema(description = "删除时执行")
    private Boolean deleteExecute;

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

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "是否删除")
    private Boolean deleted;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "状态权限配置（JSON格式）")
    private String statusPermissions;

    @Schema(description = "组织id")
    private String organizationId;
}
