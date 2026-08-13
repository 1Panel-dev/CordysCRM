package cn.cordys.crm.follow.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CommentMention {

    private String id;

    @Schema(description = "评论ID")
    private String commentId;

    @Schema(description = "用户ID")
    private String userId;
}
