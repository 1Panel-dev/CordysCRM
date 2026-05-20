package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 审批退回记录
 */
@Data
@Table(name = "approval_return_back_record")
public class ApprovalReturnBackRecord {

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "实例ID")
	private String instanceId;

	@Schema(description = "当前任务ID")
	private String taskId;

	@Schema(description = "退回至节点ID")
	private String returnToNodeId;

	@Schema(description = "退回原因")
	private String returnReason;

	@Schema(description = "退回操作人")
	private String returnUserId;
}