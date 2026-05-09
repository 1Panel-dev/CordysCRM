package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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

	@NotBlank
	@Schema(description = "当前审批执行任务ID")
	private String currentTaskId;

	@Schema(description = "审批动作", allowableValues = {"APPROVE", "REJECT", "SIGN", "BACK"})
	private String action;

	@Schema(description = "审批意见")
	private String comment;

	@Schema(description = "审批意见的附件集合")
	private List<String> attachmentIds;
}
