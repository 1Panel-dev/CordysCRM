package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_cc_task")
public class ApprovalCcTask {

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;

	@Schema(description = "抄送任务ID")
	private String taskId;

	@Schema(description = "抄送人ID")
	private String ccUserId;
}
