package cn.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeClueStatistic {
    @Schema(description = "本年度新增线索")
    private HomeStatisticSearchResponse thisYearClue;
    @Schema(description = "本月新增线索")
    private HomeStatisticSearchResponse thisMonthClue;
    @Schema(description = "本周新增线索")
    private HomeStatisticSearchResponse thisWeekClue;
    @Schema(description = "本日新增线索")
    private HomeStatisticSearchResponse todayClue;
}
