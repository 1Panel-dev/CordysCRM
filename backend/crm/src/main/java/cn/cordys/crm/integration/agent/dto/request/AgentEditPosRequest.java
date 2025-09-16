package cn.cordys.crm.integration.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentEditPosRequest {

    @Schema(description = "文件夹id")
    private String agentModuleId;

    @Schema(description = "移动智能体id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{source_id.not_blank}")
    private String moveId;

    @Schema(description = "目标智能体id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{target_id.not_blank}")
    private String targetId;

    @Schema(description = "移动类型", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"BEFORE", "AFTER", "APPEND"})
    @NotBlank(message = "{moveMode.not_blank}")
    private String moveMode;


}
