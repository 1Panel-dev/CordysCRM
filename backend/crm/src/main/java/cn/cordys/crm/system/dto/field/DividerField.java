package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "DIVIDER")
@EqualsAndHashCode(callSuper = true)
public class DividerField extends BaseField {

  @Schema(description = "分隔线样式")
  private String dividerClass;

  @Schema(description = "分隔线颜色")
  private String dividerColor;

  @Schema(description = "标题颜色")
  private String titleColor;
}
