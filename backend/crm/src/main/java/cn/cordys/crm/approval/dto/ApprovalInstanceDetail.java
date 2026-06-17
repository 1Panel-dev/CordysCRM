package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalInstanceDetail {

	@Schema(description = "审批实例ID")
	private String id;

	@Schema(description = "提交人ID")
	private String submitterId;

	@Schema(description = "提交人头像")
	private String submitAvatar;

	@Schema(description = "提交人")
	private String submitter;

	@Schema(description = "提交时间")
	private Long submitTime;

	@Schema(description = "变更说明")
	private String comment;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "当前节点ID")
	private String currentNodeId;

	@Schema(description = "执行时机：CREATE/UPDATE/DELETE")
	private String executeTime;

	@Schema(description = "审批节点")
	private List<ApprovalRecordNode> nodes;

	@Schema(description = "当前节点字段权限")
	private String currentNodeFieldPermissions;
}
