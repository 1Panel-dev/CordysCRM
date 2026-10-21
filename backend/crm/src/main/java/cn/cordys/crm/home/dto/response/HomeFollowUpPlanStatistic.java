package cn.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class HomeFollowUpPlanStatistic {

    @Schema(description = "新增跟进计划")
    private HomeStatisticSearchResponse newFollowUpPlan;
}
