package io.cordys.crm.system.dto;

import io.cordys.crm.customer.constants.RecycleConditionTimeOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
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

	/**
	 * 获取动态时间
	 * @return 动态时间
	 */
	public LocalDateTime getDynamicTime() {
		if (!StringUtils.equals(this.operator, RecycleConditionTimeOperator.DYNAMICS.name())) {
			return null;
		}
		LocalDateTime now = LocalDateTime.now();
		String[] timeArr = this.value.split(",");
		return switch (timeArr[1]) {
			case "month" -> now.minusMonths(Long.parseLong(timeArr[0]));
			case "week" -> now.minusWeeks(Long.parseLong(timeArr[0]));
			case "day" -> now.minusDays(Long.parseLong(timeArr[0]));
			default -> null;
		};
	}
}
