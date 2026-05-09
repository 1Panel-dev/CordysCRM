package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalRevokeRequest {

    @NotBlank(message = "当前task任务ID不能为空")
    @Schema(description = "task任务id")
    private String id;
}
