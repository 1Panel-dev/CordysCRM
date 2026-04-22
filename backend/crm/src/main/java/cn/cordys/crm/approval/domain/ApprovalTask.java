package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 审批任务
 */
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

	@Schema(description = "是否为加签任务")
	private Boolean isAddSign;

	@Schema(description = "是否为退回任务")
	private Boolean isReturn;

	@Schema(description = "是否为抄送任务")
	private Boolean isCc;
}
