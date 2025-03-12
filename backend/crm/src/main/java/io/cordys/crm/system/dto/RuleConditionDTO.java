package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleConditionDTO {

	@Schema(description = "列")
	private String column;
	@Schema(description = "操作符")
	private String operator;
	@Schema(description = "值")
	private String value;
	@Schema(description = "范围, 只有列为入库时间有范围值")
	private List<String> scope;
}
