package io.cordys.crm.search.response.advanced;

import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdvancedCustomerContactResponse extends CustomerContactListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

}
