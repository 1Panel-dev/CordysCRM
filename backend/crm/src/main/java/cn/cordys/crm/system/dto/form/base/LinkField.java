package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LinkField {

  @NotEmpty
  @Schema(description = "当前字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String current;

  @NotEmpty
  @Schema(description = "联动字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String link;

  @Schema(description = "是否启用联动")
  private boolean enable;
}
