package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalCommentUpdateRequest {

    @NotBlank
    @Schema(description = "评论ID")
    private String id;

    @NotBlank
    @Size(max = 3000)
    @Schema(description = "评论内容")
    private String content;

    @Size(max = 100)
    @Schema(description = "被@用户ID，全量替换；未传或为空时清空")
    private List<@NotBlank String> mentionedUserIds;

    @Size(max = 100)
    @Schema(description = "附件ID集合，全量替换；未传时保留，空列表时清空")
    private List<@NotBlank @Size(max = 32) String> attachmentIds;
}
