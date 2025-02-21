package io.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OptionProp {

	@Schema(description = "文本")
	private String label;
	@Schema(description = "值")
	private String value;
}