package cn.cordys.crm.integration.wecom.response;

import cn.cordys.crm.integration.wecom.dto.WeComUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class WeComUserListResponse extends WeComResponseEntity {
  /** 成员列表 */
  @JsonProperty("userlist")
  private List<WeComUser> userList;
}
