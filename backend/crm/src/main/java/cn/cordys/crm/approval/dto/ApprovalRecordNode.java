package cn.cordys.crm.approval.dto;

import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
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

	@Schema(description = "节点轮次")
	private Integer nodeRound;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "审批任务")
	private List<ApprovalTaskNode> taskNodes;

	@Schema(description = "多人审批方式", allowableValues = {"ALL: 会签", "ANY: 或签", "SEQUENTIAL: 依次审批"})
	private MultiApproverModeEnum multiApproverMode;

	@Schema(description = "是否退回节点")
	private boolean isReturnNode;

	@Schema(description = "抄送的节点集合")
	private List<ApprovalCcNode> ccNodes;
}
