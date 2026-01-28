package cn.cordys.crm.integration.lark.dto;

import cn.cordys.crm.integration.lark.response.LarkResponseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LarkToken extends LarkResponseEntity {

  /** 自建应用生成的accessToken。 */
  @JsonProperty("tenant_access_token")
  private String tenantAccessToken;

  /** 获取 user_access_token */
  @JsonProperty("access_token")
  private String accessToken;

  /** accessToken的过期时间，单位秒。 */
  @JsonProperty("expire")
  private String expire;
}
