package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@JsonTypeName(value = "SELECT")
@EqualsAndHashCode(callSuper = true)
public class SelectField extends BaseField {

	@Schema(description = "是否是多选")
	private Boolean multiple;

	@Schema(description = "默认值")
	private Object defaultValue;

	@Schema(description = "选项值")
	private List<OptionProp> options;
}
