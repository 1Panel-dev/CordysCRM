package cn.cordys.crm.integration.dingtalk.dto;

import cn.cordys.crm.integration.dingtalk.response.DingTalkResponseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DingTalkToken extends DingTalkResponseEntity {
    /**
     * 生成的accessToken。
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * 	accessToken的过期时间，单位秒。
     */
    @JsonProperty("expireIn")
    private String expireIn;

}
