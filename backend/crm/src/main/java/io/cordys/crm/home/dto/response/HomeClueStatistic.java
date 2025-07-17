package io.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeClueStatistic {

    @Schema(description = "线索总数")
    private Long total;

    @Schema(description = "新增线索")
    private HomeStatisticSearchResponse newClue;

    @Schema(description = "未跟进线索")
    private HomeStatisticSearchResponse unfollowedClue;

    @Schema(description = "剩余库容")
    private HomeStatisticSearchResponse remainingCapacity;
}
