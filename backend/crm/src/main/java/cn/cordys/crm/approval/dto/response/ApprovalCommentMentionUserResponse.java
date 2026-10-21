package cn.cordys.crm.approval.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "审批评论被@用户信息")
public class ApprovalCommentMentionUserResponse {

    @JsonIgnore
    @Schema(description = "用于分组的评论ID", hidden = true)
    private String commentId;

    @Schema(description = "被@用户ID")
    private String id;

    @Schema(description = "用户名称")
    private String name;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "用户在当前组织是否启用")
    private Boolean enable;
}
