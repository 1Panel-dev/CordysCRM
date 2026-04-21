package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审批执行参数
 * @author song-cc-rock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalExecuteParam {

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "审批任务ID")
	private String taskId;

	@Schema(description = "审批意见")
	private String comment;

	@Schema(description = "审批结果: 同意/驳回")
	private String result;

	@Schema(description = "审批意见的附件集合")
	private List<String> attachmentIds;
}
