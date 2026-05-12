package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalActionBatchRequest {

    @NotEmpty
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "驳回附件集合")
    private List<String> attachmentIds;

    @Schema(description="操作模块:首页/具体模块详情页")
    private String module;
}