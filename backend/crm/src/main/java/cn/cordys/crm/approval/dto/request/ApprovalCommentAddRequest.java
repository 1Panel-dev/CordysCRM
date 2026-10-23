package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalCommentAddRequest {

    @NotBlank
    @Schema(description = "审批实例ID")
    private String instanceId;

    @Schema(description = "一级评论ID，回复时必填")
    private String parentId;

    @Schema(description = "被回复人ID")
    private String replyToUserId;

    @NotBlank
    @Size(max = 3000)
    @Schema(description = "评论内容")
    private String content;

    @Size(max = 100)
    @Schema(description = "被@用户ID")
    private List<@NotBlank String> mentionedUserIds;

    @Size(max = 100)
    @Schema(description = "附件ID集合")
    private List<@NotBlank @Size(max = 32) String> attachmentIds;
}
