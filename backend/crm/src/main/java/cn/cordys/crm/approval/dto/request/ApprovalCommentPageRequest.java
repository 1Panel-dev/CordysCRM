package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批评论分页请求")
public class ApprovalCommentPageRequest extends BasePageRequest {

    @NotBlank
    @Schema(description = "审批实例ID")
    private String instanceId;
}
