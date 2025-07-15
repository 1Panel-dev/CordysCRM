package io.cordys.crm.home.dto.request;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.EnumValue;
import io.cordys.crm.home.constants.HomeStatisticPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticSearchRequest {

    @EnumValue(enumClass = BusinessSearchType.class)
    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT)")
    private String searchType;

    @Schema(description = "部门ID集合")
    private Set<String> deptIds;

    @EnumValue(enumClass = HomeStatisticPeriod.class)
    @Schema(description = "时间段(TODAY/THIS_WEEK/THIS_MONTH)")
    private String period;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;
}
