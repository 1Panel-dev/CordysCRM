package io.cordys.crm.search.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * @author guoyuqi
 * @date 2025-02-08 16:24:22
 */
@Data
public class GlobalClueResponse extends ClueListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

    @Schema(description = "意向产品名称列表")
    private List<String> productNameList;
}
