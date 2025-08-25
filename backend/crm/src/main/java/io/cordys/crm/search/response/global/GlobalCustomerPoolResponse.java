package io.cordys.crm.search.response.global;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class GlobalCustomerPoolResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "公海id")
    private String poolId;

    @Schema(description = "公海名称")
    private String poolName;

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;
}
