package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_cc_task")
public class ApprovalCcTask extends BaseModel {

	@Schema(description = "抄送任务ID")
	private String taskId;

	@Schema(description = "抄送人ID")
	private String ccUserId;
}
