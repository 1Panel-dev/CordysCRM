package cn.cordys.crm.follow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CommentResponse {
    private String id;
    private String targetType;
    private String resourceId;
    private String parentId;
    private String replyToUserId;
    private String replyToUserName;
    private String content;
    private String createUser;
    private String createUserName;
    private String createUserAvatar;
    private Long createTime;
    private Long updateTime;
    private boolean editable;
    @Schema(description = "回复数量")
    private long replyCount;
    @Schema(description = "二层回复；仅顶层评论分页接口返回")
    private List<CommentResponse> replies = List.of();
}
