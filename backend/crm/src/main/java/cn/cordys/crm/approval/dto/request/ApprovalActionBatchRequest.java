package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalActionBatchRequest {

    @NotEmpty(message = "当前任务节点ID集合不能为空")
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

	@Schema(description = "意见, 评论")
	private String comment;

	@Schema(description = "附件ID集合")
    private List<String> attachmentIds;
}