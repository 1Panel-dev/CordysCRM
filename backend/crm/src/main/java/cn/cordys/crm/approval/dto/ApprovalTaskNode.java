package cn.cordys.crm.approval.dto;

import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskNode {

	@Schema(description = "审批任务ID")
	private String taskId;

	@Schema(description = "审批人ID")
	private String approverId;

	@Schema(description = "审批人名称")
	private String approver;

	@Schema(description = "抄送人头像")
	private String approverAvatar;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "是否加签任务")
	private boolean sign;

	@Schema(description = "加签意见")
	private String signComment;

	@Schema(description = "加签附件")
	private List<Attachment> signAttachments;

	@Schema(description = "是否进行加签操作")
	private boolean signAction;

	@Schema(description = "执行记录ID")
	private String recordId;

	@Schema(description = "审批意见")
	private String comment;

	@Schema(description = "审批附件")
	private List<Attachment> attachments;

	@Schema(description = "审批完成时间")
	private Long approvalTime;
}
