package io.cordys.crm.integration.wecom.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeComCommonUserResponse extends WeComResponseEntity {

    @JsonProperty("userid")
    private String userId;

    /**
     * 成员票据，最大为512字节，有效期为1800s。scope为snsapi_privateinfo，且用户在应用可见范围之内时返回此参数。后续利用该参数可以获取用户信息或敏感信息
     */
    @JsonProperty("user_ticket")
    private String userTicket;
}
