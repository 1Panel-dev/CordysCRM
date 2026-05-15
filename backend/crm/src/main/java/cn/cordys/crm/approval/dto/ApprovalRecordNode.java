package cn.cordys.crm.approval.dto;

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

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "审批任务")
	private List<ApprovalTaskNode> taskNodes;

	@Schema(description = "是否退回节点")
	private boolean isReturnNode;

	@Schema(description = "抄送的节点集合")
	private List<ApprovalCcNode> ccNodes;
}
