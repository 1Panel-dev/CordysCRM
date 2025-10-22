package cn.cordys.crm.integration.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScriptRequest {


    @Schema(description = "工作空间id")
    @NotBlank
    private String workspaceId;

    @Schema(description = "智能体应用id")
    @NotBlank
    private String applicationId;

}
