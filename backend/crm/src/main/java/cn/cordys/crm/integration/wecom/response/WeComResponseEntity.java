package cn.cordys.crm.integration.wecom.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeComResponseEntity {

  /** 错误码 */
  @JsonProperty("errcode")
  private Integer errCode;

  /** 错误消息 */
  @JsonProperty("errmsg")
  private String errMsg;
}
