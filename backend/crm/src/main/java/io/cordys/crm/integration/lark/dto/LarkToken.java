package io.cordys.crm.integration.lark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cordys.crm.integration.lark.response.LarkResponseEntity;
import lombok.Data;

@Data
public class LarkToken extends LarkResponseEntity {

    /**
     * 自建应用生成的accessToken。
     */
    @JsonProperty("tenant_access_token")
    private String tenantAccessToken;

    /**
     * 	accessToken的过期时间，单位秒。
     */
    @JsonProperty("expire")
    private String expire;
}
