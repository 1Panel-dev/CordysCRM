package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批实例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "approval_instance")
public class ApprovalInstance extends BaseModel {

    @Schema(description = "审批流版本ID")
    private String flowVersionId;

    @Schema(description = "表单类型 quote（报价表单）、contract（合同表单）、invoice（发票表单）")
    private String type;

    @Schema(description = "审批的数据ID")
    private String resourceId;

    @Schema(description = "提交人ID")
    private String submitterId;

    @Schema(description = "当前节点ID")
    private String currentNodeId;

    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "执行时机：CREATE/UPDATE/DELETE")
    private String executeTime;

    @Schema(description = "提审时间")
    private Long submitTime;

	@Schema(description = "审批完成时间")
	private Long approvalTime;

    @Schema(description = "变更说明")
    private String comment;

    @Schema(description = "编辑时修改的字段列表")
    private String updateFields;
}
