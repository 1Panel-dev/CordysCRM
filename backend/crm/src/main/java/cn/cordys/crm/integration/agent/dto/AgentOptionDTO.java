package cn.cordys.crm.integration.agent.dto;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AgentOptionDTO extends OptionDTO {

  @Schema(description = "脚本")
  private String script;
}
