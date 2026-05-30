package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalRevokeRequest {

    @NotBlank(message = "撤回的节点任务不能为空")
    @Schema(description = "撤回的节点任务ID")
    private String id;
}
