package cn.cordys.crm.integration.dingtalk.response;

import cn.cordys.crm.integration.dingtalk.dto.DingTalkUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** 钉钉用户列表响应 */
@Data
public class DingTalkUserListResponse {

  @JsonProperty("request_id")
  private String requestId;

  private List<DingTalkUser> result;
}
