package io.cordys.crm.integration.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cordys.crm.integration.dingtalk.response.DingTalkResponseEntity;
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
