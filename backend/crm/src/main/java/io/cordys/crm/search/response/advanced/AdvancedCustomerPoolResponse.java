package io.cordys.crm.search.response.advanced;

import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdvancedCustomerPoolResponse extends CustomerListResponse {

    @Schema(description = "公海名称")
    private String poolName;


    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;
}