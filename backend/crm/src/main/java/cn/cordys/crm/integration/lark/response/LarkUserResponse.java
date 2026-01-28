package cn.cordys.crm.integration.lark.response;

import cn.cordys.crm.integration.lark.dto.LarkUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author guoyuqi
 * @date 2024-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LarkUserResponse extends LarkResponseEntity {

  @JsonProperty("data")
  private LarkUserData data;

  @Data
  public static class LarkUserData {
    @JsonProperty("items")
    private List<LarkUser> items;

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("page_token")
    private String pageToken;
  }
}
