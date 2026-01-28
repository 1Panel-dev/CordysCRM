package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.LinkProp;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "SELECT")
@EqualsAndHashCode(callSuper = true)
public class SelectField extends BaseField implements HasOption {

  @Schema(description = "默认值")
  private String defaultValue;

  @Schema(description = "选项值")
  private List<OptionProp> options;

  @Schema(description = "自定义选项值")
  private List<OptionProp> customOptions;

  @Schema(
      description = "选项来源",
      allowableValues = {"custom", "ref"})
  private String optionSource;

  @Schema(description = "选项引用ID (optionSource=ref 时生效)")
  private String refId;

  @Schema(description = "引用表单key (optionSource=ref 时生效)")
  private String refFormKey;

  @Schema(description = "联动属性")
  private LinkProp linkProp;
}
