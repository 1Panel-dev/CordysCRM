package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "clue_pool_recycle_rule")
public class CluePoolRecycleRule extends BaseModel {

  @Schema(description = "线索池ID")
  private String poolId;

  @Schema(description = "操作符")
  private String operator;

  @Schema(description = "回收条件")
  private String condition;
}
