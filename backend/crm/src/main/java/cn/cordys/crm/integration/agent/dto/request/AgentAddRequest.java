package cn.cordys.crm.integration.agent.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class AgentAddRequest {

  @Size(
      min = 1,
      max = 255,
      message = "{department.name.length_range}",
      groups = {Created.class, Updated.class})
  @NotBlank(
      message = "{dashboard.name.not_blank}",
      groups = {Created.class, Updated.class})
  @Schema(description = "智能体名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "文件夹id")
  @NotBlank(message = "{dashboard.parent.id.not_blank}")
  private String agentModuleId;

  @NotNull
  @Schema(description = "范围ID集合")
  private List<String> scopeIds;

  @NotBlank
  @Schema(description = "嵌入脚本")
  private String script;

  @Schema(description = "描述")
  private String description;

  @Schema(description = "添加方式")
  @NotBlank
  private String type;

  @Schema(description = "工作空间id")
  private String workspaceId;

  @Schema(description = "智能体应用id")
  private String applicationId;
}
