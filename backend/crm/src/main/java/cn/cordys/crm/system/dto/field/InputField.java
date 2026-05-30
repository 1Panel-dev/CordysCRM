package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "INPUT")
@EqualsAndHashCode(callSuper = true)
public class InputField extends BaseField {

    @Schema(description = "默认值")
    private String defaultValue;

	@Schema(description = "默认值类型", allowableValues = {"custom", "formula"})
	private String defaultValueType;

	@Schema(description = "公式")
	private String formula;
}
