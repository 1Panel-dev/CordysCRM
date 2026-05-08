package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalReturnBackRequest extends ApprovalOperationBaseRequest {

    @NotBlank(message = "退回至任务ID不能为空")
    @Schema(description = "退回至任务ID")
    private String returnToTaskId;

    @Schema(description = "退回原因")
    private String returnReason;

    @Schema(description = "加签意见的附件集合")
    private List<String> attachmentIds;
}
