package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "TEXTAREA")
@EqualsAndHashCode(callSuper = true)
public class TextAreaField extends BaseField {

	@Schema(description = "默认值")
	private String defaultValue;
}
