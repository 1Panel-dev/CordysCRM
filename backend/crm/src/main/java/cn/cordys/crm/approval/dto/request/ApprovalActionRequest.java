package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApprovalActionRequest {

    @NotBlank(message = "当前任务节点ID不能为空")
    @Schema(description = "当前任务节点ID")
    private String id;

    @NotBlank(message = "当前节点ID不能为空")
    @Schema(description = "节点ID")
    private String nodeId;

    @NotBlank(message = "审批实例ID不能为空")
    @Schema(description = "审批实例ID")
    private String instanceId;

	@NotBlank(message = "审批人ID不能为空")
    @Schema(description = "审批人ID")
    private String approverId;

	@Schema(description = "意见, 评论")
	private String comment;

	@Schema(description = "附件ID集合")
	private List<String> attachmentIds;

	public String getNodeId() {
		if (nodeId.contains("-SN")) {
			return nodeId.split("-SN")[0];
		}
		return nodeId;
	}
}
