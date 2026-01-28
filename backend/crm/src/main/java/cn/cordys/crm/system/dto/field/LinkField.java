package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "LINK")
@EqualsAndHashCode(callSuper = true)
public class LinkField extends BaseField {

  @Schema(description = "链接来源")
  private String linkSource;

  @Schema(description = "打开方式")
  private String openMode;
}
