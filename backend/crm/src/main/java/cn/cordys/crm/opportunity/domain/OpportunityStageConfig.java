package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_stage_config")
public class OpportunityStageConfig extends BaseModel {

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

  @Schema(description = "组织id")
  private String organizationId;
}
