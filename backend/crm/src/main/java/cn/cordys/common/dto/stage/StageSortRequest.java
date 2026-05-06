package cn.cordys.common.dto.stage;

import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StageSortRequest extends NodeMoveRequest {

    @NotBlank
    @Schema(description = "阶段", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stage;

}
