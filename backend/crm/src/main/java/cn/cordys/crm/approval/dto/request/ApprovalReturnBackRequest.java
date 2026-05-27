package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalReturnBackRequest extends ApprovalActionRequest {

    @NotBlank(message = "退回至任务ID不能为空")
    @Schema(description = "退回至节点ID")
    private String returnToNodeId;
}
