package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 加签任务
 */
@Data
@Table(name = "approval_add_sign_task")
public class ApprovalAddSignTask extends BaseModel {

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "加签任务ID")
	private String taskId;

	@Schema(description = "审批人")
	private String approverId;

	@Schema(description = "加签方式")
	private String type;

	@Schema(description = "加签位置")
	private Integer pos;

	@Schema(description = "加签意见")
	private String comment;

	@Schema(description = "状态")
	private String status;
}
