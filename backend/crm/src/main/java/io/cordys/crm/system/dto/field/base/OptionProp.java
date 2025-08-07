package io.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionProp {

	@Schema(description = "值")
	private String value;
	@Schema(description = "文本")
	private String label;
}