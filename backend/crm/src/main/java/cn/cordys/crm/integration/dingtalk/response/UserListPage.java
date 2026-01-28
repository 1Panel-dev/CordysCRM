package cn.cordys.crm.integration.dingtalk.response;

import cn.cordys.crm.integration.dingtalk.dto.DingTalkUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** 钉钉用户列表分页 */
@Data
public class UserListPage {
  @JsonProperty("has_more")
  private Boolean hasMore;

  @JsonProperty("next_cursor")
  private Long nextCursor;

  private List<DingTalkUser> list;
}
