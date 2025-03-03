package io.cordys.crm.follow.dto.request;

import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FollowUpRecordPageRequest extends BasePageRequest {

    @Schema(description = "资源id: 客户id/商机id/线索id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceId;
}
