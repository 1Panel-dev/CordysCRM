package cn.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用子表格字段(兼容子表格)
 *
 * @author song-cc-rock
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubField extends BaseField {

  @Schema(description = "固定宽度")
  private Integer fixedColumn;

  @Schema(description = "表格子字段")
  private List<BaseField> subFields;

  @Schema(description = "汇总列")
  private List<String> sumColumns;
}
