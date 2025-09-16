package cn.cordys.crm.integration.agent.dto.response;

import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AgentDetailResponse {

    @Schema(description = "id")
    private String id;

    @Schema(description = "仪表板名称")
    private String name;

    @Schema(description = "文件夹id")
    private String agentModuleId;

    @Schema(description = "文件夹名称")
    private String agentModuleName;

    @Schema(description = "应用范围")
    private String scopeId;

    @Schema(description = "应用范围集合")
    private List<ScopeNameDTO> members;

    @Schema(description = "嵌入脚本")
    private String script;

    @Schema(description = "描述")
    private String description;
}
