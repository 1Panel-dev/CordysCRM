package cn.cordys.crm.clue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CluePoolFieldConfigDTO {
  @Schema(description = "字段ID")
  private String fieldId;

  @Schema(description = "字段名称")
  private String fieldName;

  @Schema(description = "是否启用")
  private Boolean enable;

  @Schema(description = "是否可编辑")
  private Boolean editable;
}
