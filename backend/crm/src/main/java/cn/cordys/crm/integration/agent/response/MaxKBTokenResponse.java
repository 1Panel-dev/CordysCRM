package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MaxKBTokenResponse extends MaxKBResponseEntity {

  @JsonProperty("data")
  TokenResponse data;
}
