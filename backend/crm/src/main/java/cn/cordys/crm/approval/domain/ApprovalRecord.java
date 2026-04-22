package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.persistence.Table;

/**
 * 审批记录
 */
@Data
@Table(name = "approval_record")
public class ApprovalRecord extends BaseModel {

	@Schema(description = "审批实例ID")
	private String instanceId;

	@Schema(description = "任务ID")
	private String taskId;

	@Schema(description = "节点ID")
	private String nodeId;

	@Schema(description = "审批结果")
	private String result;

	@Schema(description = "审批意见")
	private String comment;
}