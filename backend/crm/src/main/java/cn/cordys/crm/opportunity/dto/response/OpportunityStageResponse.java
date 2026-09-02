package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.common.dto.stage.StageConfigResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OpportunityStageResponse extends StageConfigResponse {


    @Schema(description = "赢率")
    private String rate;
}
