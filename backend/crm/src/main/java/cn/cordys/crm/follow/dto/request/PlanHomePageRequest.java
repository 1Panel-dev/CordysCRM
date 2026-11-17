package cn.cordys.crm.follow.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PlanHomePageRequest extends BasePageRequest {

    @Schema(description = "筛选状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
