package cn.cordys.crm.integration.agent.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AgentPageRequest extends BasePageRequest {

    @Schema(description = "文件ids")
    private List<String> agentModuleIds;

}
