package cn.cordys.crm.follow.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FollowUpRecordPageRequest extends BasePageRequest {

    @Schema(description = "资源id: 客户id/商机id/线索id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceId;

    @Schema(description = "转移的资源ID集合")
    private List<String> transitionIds;

    @Schema(description = "引用的线索ID集合")
    private List<String> refClueIds;

    @Schema(description = "引用的客户ID")
    private String refCustomerId;

    @Schema(description = "引用的用户ID")
    private String refUserId;
}
