package cn.cordys.crm.approval.dto;

import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
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

	@Schema(description = "节点ID")
	private String nodeId;

	@Schema(description = "节点名称")
	private String nodeName;

	@Schema(description = "节点轮次")
	private Integer nodeRound;

	@Schema(description = "序号")
	private Integer sort;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "审批任务")
	private List<ApprovalTaskNode> taskNodes;

	@Schema(description = "多人审批方式", allowableValues = {"ALL: 会签", "ANY: 或签", "SEQUENTIAL: 依次审批"})
	private MultiApproverModeEnum multiApproverMode;

	@Schema(description = "是否结束节点")
	private boolean endNode;

	@Schema(description = "是否退回节点")
	private boolean backNode;

	@Schema(description = "退回原因")
	private String backReason;

	@Schema(description = "退回附件")
	private List<Attachment> backAttachments;

	@Schema(description = "抄送的节点集合")
	private List<ApprovalCcNode> ccNodes;
}
