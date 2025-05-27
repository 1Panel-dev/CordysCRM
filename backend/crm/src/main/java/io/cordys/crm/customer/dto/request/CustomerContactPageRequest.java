package io.cordys.crm.customer.dto.request;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.EnumValue;
import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;



/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactPageRequest extends BasePageRequest {

    @EnumValue(enumClass = BusinessSearchType.class)
    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT/VISIBLE)")
    private String searchType;
}
