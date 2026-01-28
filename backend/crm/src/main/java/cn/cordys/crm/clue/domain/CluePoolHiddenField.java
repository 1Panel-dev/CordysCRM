package cn.cordys.crm.clue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "clue_pool_hidden_field")
public class CluePoolHiddenField {

  @Schema(description = "线索池ID")
  private String poolId;

  @Schema(description = "字段ID")
  private String fieldId;
}
