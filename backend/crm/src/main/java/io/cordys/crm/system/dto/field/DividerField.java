package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "DIVIDER")
@EqualsAndHashCode(callSuper = true)
public class DividerField extends BaseField {

	@Schema(description = "分隔线颜色")
	private String splitColor;
	@Schema(description = "标题颜色")
	private String labelColor;
}
