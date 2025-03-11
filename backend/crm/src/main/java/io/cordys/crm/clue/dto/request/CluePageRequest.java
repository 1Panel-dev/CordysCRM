package io.cordys.crm.clue.dto.request;

import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CluePageRequest extends BasePageRequest {

    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT/VISIBLE/CUSTOMER_TRANSITION/OPPORTUNITY_TRANSITION)")
    private String searchType ;

}
