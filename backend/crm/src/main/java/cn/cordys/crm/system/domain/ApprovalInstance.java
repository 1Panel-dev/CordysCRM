package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_instance")
public class ApprovalInstance extends BaseModel {

	@Schema(description = "流程ID")
	private String flowId;

	@Schema(description = "表单类型 quote（报价表单）、contract（合同表单）、invoice（发票表单）")
	private String type;

	@Schema(description = "审批的数据ID")
	private String resourceId;

	@Schema(description = "提交人ID")
	private String submitterId;

	@Schema(description = "当前节点id")
	private String currentNodeId;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "提审时间")
	private Long submitTime;

	@Schema(description = "审批完成时间")
	private Long approveTime;

	@Schema(description = "审批结果")
	private String result;
}
