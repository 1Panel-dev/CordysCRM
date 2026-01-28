package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MaxKBApplicationResponse extends MaxKBResponseEntity {

  @JsonProperty("data")
  ApplicationDetail data;
}
