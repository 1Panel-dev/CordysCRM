package io.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeCustomerStatistic {

    @Schema(description = "客户总数")
    private Long total;

    @Schema(description = "新增客户")
    private HomeStatisticSearchResponse newCustomer;

    @Schema(description = "未跟进客户")
    private HomeStatisticSearchResponse unfollowedCustomer;

    @Schema(description = "剩余库容")
    private Long remainingCapacity;

    @Schema(description = "库容未配置")
    private Boolean unConfigured;
}
