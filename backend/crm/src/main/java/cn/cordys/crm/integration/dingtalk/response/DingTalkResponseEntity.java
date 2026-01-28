package cn.cordys.crm.integration.dingtalk.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DingTalkResponseEntity {

  /** 错误码 */
  @JsonProperty("errcode")
  private Integer errCode;

  /** 错误消息 */
  @JsonProperty("errmsg")
  private String errMsg;
}
