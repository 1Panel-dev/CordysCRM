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
public class ApprovalRecordNode {

	@Schema(description = "审批任务ID")
	private String taskId;

	@Schema(description = "审批记录ID")
	private String recordId;

	@Schema(description = "节点ID")
	private String nodeId;

	@Schema(description = "审批人ID")
	private String approverId;

	@Schema(description = "审批人名称")
	private String approver;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "提交审批时间")
	private Long approvalTime;

	@Schema(description = "审批意见")
	private String comment;

	@Schema(description = "审批附件")
	private List<Attachment> attachments;

	@Schema(description = "是否退回节点")
	private boolean isReturnNode;

	@Schema(description = "是否加签节点")
	private boolean isAddSignNode;

	@Schema(description = "抄送的节点集合")
	private List<ApprovalCcNode> ccNodes;
}
