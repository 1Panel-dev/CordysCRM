package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "INPUT_NUMBER")
@EqualsAndHashCode(callSuper = true)
public class InputNumberField extends BaseField {

	@Schema(description = "格式", allowableValues = {"percent", "number"})
	private String format;

	@Schema(description = "保留小数点位数")
	private Boolean keepPrecision;

	@Schema(description = "位数")
	private int precision;

	@Schema(description = "显示千分位")
	private Boolean formatWithComma;
}
