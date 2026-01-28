package cn.cordys.crm.opportunity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StageConfigResponse {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "值")
  private String name;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "赢率")
  private String rate;

  @Schema(description = "进行中回退设置")
  private Boolean afootRollBack;

  @Schema(description = "完结回退设置")
  private Boolean endRollBack;

  @Schema(description = "顺序")
  private Long pos;

  @Schema(description = "当前阶段是否存在数据")
  private Boolean stageHasData = false;
}
