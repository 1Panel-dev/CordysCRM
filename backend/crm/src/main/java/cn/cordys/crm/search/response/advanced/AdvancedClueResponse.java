package cn.cordys.crm.search.response.advanced;

import cn.cordys.crm.clue.dto.response.ClueListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * @author guoyuqi
 * @date 2025-02-08 16:24:22
 */
@Data
public class AdvancedClueResponse extends ClueListResponse {

  @Schema(description = "是否有当前数据的权限")
  private boolean hasPermission;

  @Schema(description = "意向产品名称列表")
  private List<String> productNameList;
}
