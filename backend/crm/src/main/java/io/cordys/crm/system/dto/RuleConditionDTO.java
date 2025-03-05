package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class RuleConditionDTO {

	@Schema(description = "列")
	private String column;
	@Schema(description = "操作符")
	private String operator;
	@Schema(description = "值")
	private String value;
	@Schema(description = "范围")
	private String scope;
}
