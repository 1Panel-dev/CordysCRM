package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公式字段
 *
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "FORMULA")
@EqualsAndHashCode(callSuper = true)
public class FormulaField extends BaseField {

  @Schema(description = "公式")
  private String formula;

  @Schema(description = "显示千分位")
  private Boolean showThousandsSeparator;
}
