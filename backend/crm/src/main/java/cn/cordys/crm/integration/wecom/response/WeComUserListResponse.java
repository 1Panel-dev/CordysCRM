package cn.cordys.crm.integration.wecom.response;

import cn.cordys.crm.integration.wecom.dto.WeComUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WeComUserListResponse extends WeComResponseEntity {
    /**
     * 成员列表
     */
    @JsonProperty("userlist")
    private List<WeComUser> userList;

}
