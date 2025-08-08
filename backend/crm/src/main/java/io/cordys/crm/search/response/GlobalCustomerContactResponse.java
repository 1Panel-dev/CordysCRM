package io.cordys.crm.search.response;

import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GlobalCustomerContactResponse extends CustomerContactListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

}
