package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserViewListResponse {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "视图名称")
  private String name;

  @Schema(description = "是否固定")
  private Boolean fixed;

  @Schema(description = "状态")
  private Boolean enable;
}
