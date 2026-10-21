package cn.cordys.crm.search.response.advanced;

import cn.cordys.crm.clue.dto.response.ClueListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author guoyuqi
 */
@Data
public class AdvancedClueResponse extends ClueListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

    @Schema(description = "意向产品名称列表")
    private List<String> productNameList;
}
