package io.cordys.crm.integration.wecom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cordys.crm.integration.wecom.response.WeComResponseEntity;
import lombok.Data;

@Data
public class WeComToken extends WeComResponseEntity {


    /**
     * 获取到的凭证，最长为512字节
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 	凭证的有效时间（秒）
     */
    @JsonProperty("expires_in")
    private String expiresIn;

}
