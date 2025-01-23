package io.cordys.integration.wecom.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cordys.integration.wecom.dto.WeComUser;
import lombok.Data;

import java.util.List;

@Data
public class UserListResponse extends WeComResponseEntity {
    /**
     * 成员列表
     */
    @JsonProperty("userlist")
    private List<WeComUser> userList;
}
