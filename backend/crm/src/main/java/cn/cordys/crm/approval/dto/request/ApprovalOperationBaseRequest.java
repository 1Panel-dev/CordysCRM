package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalOperationBaseRequest {

    @NotBlank(message = "当前task任务ID不能为空")
    @Schema(description = "task任务id")
    private String id;

    @NotBlank(message = "当前node节点id不能为空")
    @Schema(description = "node节点id")
    private String nodeId;

    @NotBlank(message = "审批实例ID不能为空")
    @Schema(description = "审批实例ID")
    private String instanceId;

    @Schema(description = "审批人ID")
    private String approverId;
}
