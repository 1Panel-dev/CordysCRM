package io.cordys.crm.search.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author guoyuqi
 * @date 2025-02-08 16:24:22
 */
@Data
public class GlobalClueResponse extends ClueListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;
}
