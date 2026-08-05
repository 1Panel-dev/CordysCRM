package cn.cordys.crm.follow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Comment extends BaseModel {

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "顶层评论ID，顶层评论为空")
    private String parentId;

    @Schema(description = "被回复人用户ID")
    private String replyToUserId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "组织ID")
    private String organizationId;
}
