package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MaxKBResponseEntity {

  /** 错误码 */
  @JsonProperty("code")
  private int code;

  /** 错误消息 */
  @JsonProperty("message")
  private String message;
}
