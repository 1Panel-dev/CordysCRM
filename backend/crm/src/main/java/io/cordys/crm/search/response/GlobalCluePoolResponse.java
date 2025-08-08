package io.cordys.crm.search.response;

import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GlobalCluePoolResponse extends ClueListResponse {

    @Schema(description = "线索池名称")
    private String poolName;

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;
}