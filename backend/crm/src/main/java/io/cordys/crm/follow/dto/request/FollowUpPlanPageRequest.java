package io.cordys.crm.follow.dto.request;

import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FollowUpPlanPageRequest extends BasePageRequest {

    @Schema(description = "资源id: 客户id/商机id/线索id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceId;

    @Schema(description = "状态: ALL/PREPARED/UNDERWAY/COMPLETED/CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "是否是为我的计划(默认false,个人中心查询时传入true)")
    private boolean myPlan = false;
}
