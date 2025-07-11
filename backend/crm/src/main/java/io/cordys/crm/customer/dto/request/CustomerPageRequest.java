package io.cordys.crm.customer.dto.request;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.EnumValue;
import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerPageRequest extends BasePageRequest {

    @EnumValue(enumClass = BusinessSearchType.class)
    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT/VISIBLE)")
    private String searchType ;

    @Schema(description = "公海ID{公海客户列表时传参}")
    private String poolId;

    @Schema(description = "是否查询转移客户列表")
    private Boolean transition;

    @Schema(description = "查询转移需要的公海客户时传参")
    private List<String> transitionPoolIds;
}
