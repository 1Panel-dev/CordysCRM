package io.cordys.crm.opportunity.dto.request;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.EnumValue;
import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OpportunityPageRequest extends BasePageRequest {

    @EnumValue(enumClass = BusinessSearchType.class)
    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT/DEAL)")
    private String searchType ;
}
