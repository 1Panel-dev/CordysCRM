package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageUpdateRequest {

    @Schema(description = "id")
    private String id;

    @Schema(description = "值")
    private String name;

    @Schema(description = "赢率")
    private String rate;
}

