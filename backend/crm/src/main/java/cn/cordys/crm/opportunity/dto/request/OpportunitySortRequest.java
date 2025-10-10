package cn.cordys.crm.opportunity.dto.request;

import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpportunitySortRequest extends NodeMoveRequest {

    @NotBlank
    @Schema(description = "商机阶段", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stage;

}
