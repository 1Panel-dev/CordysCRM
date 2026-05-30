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

	@Schema(description = "节点轮次")
	private Integer nodeRound;

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "审批人ID")
	private String approverId;

	@Schema(description = "任务状态")
	private String status;

	@Schema(description = "任务类型")
	private String type;

	@Schema(description = "执行操作")
	private String action;
}
