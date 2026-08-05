package cn.cordys.crm.follow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CommentAddRequest {

    @NotBlank
    @Schema(description = "跟进计划或跟进记录ID")
    private String resourceId;

    @Schema(description = "顶层评论ID，回复时必填")
    private String parentId;

    @Schema(description = "被回复人用户ID")
    private String replyToUserId;

    @NotBlank
    @Size(max = 3000)
    @Schema(description = "评论内容")
    private String content;

    @Size(max = 100)
    @Schema(description = "被@用户ID")
    private List<String> mentionedUserIds;
}
