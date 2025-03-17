package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "DATE_TIME")
@EqualsAndHashCode(callSuper = true)
public class DateTimeField extends BaseField {

	@Schema(description = "日期类型|格式", allowableValues = {"month", "date", "datetime"})
	private String dateType;

	@Schema(description = "默认值")
	private Long defaultValue;
}
