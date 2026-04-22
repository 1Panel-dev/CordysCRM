package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 审批退回记录
 */
@Data
@Table(name = "approval_return_back_record")
public class ApprovalReturnBackRecord extends BaseModel {

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "当前任务ID")
	private String taskId;

	@Schema(description = "退回至任务ID")
	private String returnToTaskId;

	@Schema(description = "退回原因")
	private String returnReason;

	@Schema(description = "退回操作人")
	private String returnUserId;
}