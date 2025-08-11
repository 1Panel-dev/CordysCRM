package io.cordys.crm.search.response;

import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GlobalCustomerResponse extends CustomerListResponse {

    @Schema(description = "线索重复数据数量")
    private Integer clueCount;

    @Schema(description = "商机重复数据数量")
    private Integer opportunityCount;

    @Schema(description = "线索模块是否开启")
    private boolean clueModuleEnable;

    @Schema(description = "商机模块是否开启")
    private boolean opportunityModuleEnable;

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

}
