package cn.cordys.crm.follow.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class CommentMentionUserResponse {

    @JsonIgnore
    private String commentId;

    private String id;

    private String name;

    private String avatar;

    private Boolean enable;
}
