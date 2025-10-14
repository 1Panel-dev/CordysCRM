package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageRollBackRequest {

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack;
}
