package cn.cordys.crm.opportunity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class StageConfigListResponse {

  @Schema(description = "商机阶段配置列表")
  List<StageConfigResponse> stageConfigList;

  @Schema(description = "进行中回退设置")
  private Boolean afootRollBack = true;

  @Schema(description = "完结回退设置")
  private Boolean endRollBack = false;
}
