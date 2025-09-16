package cn.cordys.crm.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AgentLogDTO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件夹名称")
    private String AgentModuleName;

    @Schema(description = "可查看人员集合")
    private List<String> members;

    @Schema(description = "嵌入脚本")
    private String script;

    @Schema(description = "描述")
    private String description;
}
