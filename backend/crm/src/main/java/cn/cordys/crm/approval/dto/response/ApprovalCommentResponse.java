package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "审批评论及回复")
public class ApprovalCommentResponse {

    @Schema(description = "评论ID")
    private String id;

    @Schema(description = "业务资源ID")
    private String resourceId;

    @Schema(description = "所属一级评论ID，一级评论自身为空")
    private String parentId;

    @Schema(description = "被回复人ID，一级评论为空")
    private String replyToUserId;

    @Schema(description = "被回复人名称，一级评论为空")
    private String replyToUserName;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论人ID")
    private String createUser;

    @Schema(description = "评论人名称")
    private String createUserName;

    @Schema(description = "评论人头像")
    private String createUserAvatar;

    @Schema(description = "评论创建时间，毫秒时间戳")
    private Long createTime;

    @Schema(description = "评论更新时间，毫秒时间戳")
    private Long updateTime;

    @Schema(description = "当前用户是否为评论创建人，可编辑和删除评论")
    private boolean editable;

    @Schema(description = "一级评论下的二级回复数量，二级评论为0")
    private long replyCount;

    @Schema(description = "评论中@的用户列表")
    private List<ApprovalCommentMentionUserResponse> mentionUsers = List.of();

    @Schema(description = "评论附件")
    private List<AttachmentResponse> attachments = List.of();

    @Schema(description = "一级评论的二级回复集合，二级评论为空列表")
    private List<ApprovalCommentResponse> replies = List.of();

    @Data
    @Schema(description = "审批评论附件")
    public static class AttachmentResponse {

        @Schema(description = "附件ID")
        private String id;

        @Schema(description = "附件名称")
        private String name;

        @Schema(description = "附件类型")
        private String type;

        @Schema(description = "附件大小，字节")
        private Long size;
    }
}
