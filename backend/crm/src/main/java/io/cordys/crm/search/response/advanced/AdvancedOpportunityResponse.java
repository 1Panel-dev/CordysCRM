package io.cordys.crm.search.response.advanced;

import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdvancedOpportunityResponse extends OpportunityListResponse {

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;
}