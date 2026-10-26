package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "approval_comment")
public class ApprovalComment extends BaseModel {

    @Schema(description = "业务资源ID")
    private String resourceId;

    @Schema(description = "一级评论ID，一级评论自身为空")
    private String parentId;

    @Schema(description = "被回复人ID")
    private String replyToUserId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "组织ID")
    private String organizationId;
}
