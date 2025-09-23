package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.LinkProp;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@JsonTypeName(value = "SELECT_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class SelectMultipleField extends BaseField implements HasOption {

    @Schema(description = "默认值")
    private List<String> defaultValue;

    @Schema(description = "选项值")
    private List<OptionProp> options;

    @Schema(description = "联动属性")
    private LinkProp linkProp;
}
