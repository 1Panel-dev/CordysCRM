package cn.cordys.crm.integration.agent.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentModuleAddRequest {

    @Size(min = 1, max = 255, message = "{agent.name.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{agent_module.name.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;


    @Schema(description = "父级id")
    @NotBlank(message = "{agent_module.parent.id.not_blank}")
    private String parentId;

}
