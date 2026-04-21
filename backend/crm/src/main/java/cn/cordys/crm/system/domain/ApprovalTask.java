package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_task")
public class ApprovalTask extends BaseModel {

	@Schema(description = "节点ID")
	private String nodeId;

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "审批人ID")
	private String approverId;

	@Schema(description = "任务状态")
	private String taskStatus;

	@Schema(description = "审批方式")
	private String approvalMethod;

	@Schema(description = "是否加签")
	private Boolean isAddSign;

	@Schema(description = "加签人")
	private String addSignBy;

	@Schema(description = "加签时间")
	private Long addSignTime;

	@Schema(description = "是否退回")
	private Boolean isReturn;

	@Schema(description = "退回至节点")
	private String returnToNodeId;

	@Schema(description = "退回原因")
	private String returnReason;

	@Schema(description = "退回人")
	private String returnBy;

	@Schema(description = "退回时间")
	private Long returnTime;

	@Schema(description = "是否为抄送任务")
	private Boolean isCc;
}
