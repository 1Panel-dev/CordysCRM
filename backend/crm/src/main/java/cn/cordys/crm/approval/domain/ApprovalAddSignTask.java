package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 加签任务
 */
@Data
@Table(name = "approval_add_sign_task")
public class ApprovalAddSignTask {

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "加签任务ID")
	private String taskId;

	@Schema(description = "加签的节点ID")
	private String signTaskId;

	@Schema(description = "加签方式")
	private String type;

	@Schema(description = "根任务ID(同一加签链的根节点)")
	private String rootTaskId;

	@Schema(description = "顺序")
	private Long sort;

	@Schema(description = "加签意见")
	private String comment;
}
