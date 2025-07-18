package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class FilterConditionDTO {

	@Schema(description = "列")
	private String column;
	@Schema(description = "操作符")
	private String operator;
	@Schema(description = "值")
	private String value;
}
