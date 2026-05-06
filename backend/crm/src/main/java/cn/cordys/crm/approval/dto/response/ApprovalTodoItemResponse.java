package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalTodoItemResponse {

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "提交时间")
    private Long submitTime;
}
