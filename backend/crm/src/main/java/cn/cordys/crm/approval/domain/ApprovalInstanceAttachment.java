package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_instance_attachment")
public class ApprovalInstanceAttachment extends BaseModel {

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "审批节点ID")
	private String approvalElementId;

	@Schema(description = "附件ID")
	private String attachmentId;
}
