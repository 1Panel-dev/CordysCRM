package io.cordys.common.utils;

import io.cordys.crm.customer.constants.RecycleConditionColumnKey;
import io.cordys.crm.customer.constants.RecycleConditionScopeKey;
import io.cordys.crm.customer.constants.RecycleConditionTimeOperator;
import io.cordys.crm.system.dto.RuleConditionDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 条件工具类
 */
public class RecycleConditionUtils {

	public static final int MAX_CONDITION_SCOPE_SIZE = 2;

	public static Integer calcMinRecycleDays(List<RuleConditionDTO> conditions, Long createTime, Long collectionTime) {
		List<RuleConditionDTO> reservedConditions = conditions.stream().filter(condition -> RecycleConditionColumnKey.matchReserved(condition.getColumn())).toList();
		if (CollectionUtils.isEmpty(reservedConditions)) {
			return null;
		}
		RuleConditionDTO condition = reservedConditions.getFirst();
		if (StringUtils.equals(condition.getOperator(), RecycleConditionTimeOperator.FIXED.name())) {
			return null;
		}
		LocalDateTime dynamicTime = condition.getDynamicTime();
		if (dynamicTime == null) {
			return null;
		}
		LocalDateTime minPickedTime;
		if (condition.getScope().size() == MAX_CONDITION_SCOPE_SIZE) {
			minPickedTime = Instant.ofEpochMilli(Math.min(createTime, collectionTime)).atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else if (condition.getScope().contains(RecycleConditionScopeKey.CREATED)) {
			minPickedTime = Instant.ofEpochMilli(createTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else if (condition.getScope().contains(RecycleConditionScopeKey.PICKED)) {
			minPickedTime = Instant.ofEpochMilli(collectionTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else {
			return null;
		}
		long betweenDays = ChronoUnit.DAYS.between(dynamicTime, minPickedTime);
		if (!minPickedTime.toLocalTime().equals(LocalDateTime.MIN.toLocalTime())) {
			betweenDays++;
		}
		return (int) betweenDays;
	}
}
