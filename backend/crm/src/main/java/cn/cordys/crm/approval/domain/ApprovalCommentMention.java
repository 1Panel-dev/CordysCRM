package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_comment_mention")
public class ApprovalCommentMention {

    @Id
    @Schema(description = "ID")
    private String id;

    @Schema(description = "评论ID")
    private String commentId;

    @Schema(description = "被@用户ID")
    private String userId;

    @Schema(description = "被@时间，毫秒时间戳")
    private Long createTime;
}
