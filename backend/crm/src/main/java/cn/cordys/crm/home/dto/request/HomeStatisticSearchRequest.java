package cn.cordys.crm.home.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.home.constants.HomeStatisticPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticSearchRequest extends HomeStatisticBaseSearchRequest {

    @EnumValue(enumClass = HomeStatisticPeriod.class)
    @Schema(description = "时间段(TODAY/THIS_WEEK/THIS_MONTH/THIS_YEAR)")
    private String period;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;
}
