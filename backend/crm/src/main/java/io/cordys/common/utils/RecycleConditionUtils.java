package io.cordys.common.utils;

import io.cordys.crm.customer.constants.RecycleConditionColumnKey;
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
 * 回收规则工具类
 */
public class RecycleConditionUtils {

	private static final int FIX_TIME_LEN = 2;

	/**
	 * 计算回收天数
	 * @param conditions 回收条件
	 * @param reserveTime 回收时间
	 * @return 回收天数
	 */
	public static Integer calcRecycleDays(List<RuleConditionDTO> conditions, Long reserveTime) {
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
		LocalDateTime pickedTime = Instant.ofEpochMilli(reserveTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
		long betweenDays = ChronoUnit.DAYS.between(dynamicTime, pickedTime);
		if (!pickedTime.toLocalTime().equals(LocalDateTime.MIN.toLocalTime())) {
			betweenDays++;
		}
		return (int) betweenDays;
	}

	/**
	 * 匹配回收时间
	 * @param condition 回收条件
	 * @param matchTime 匹配时间
	 * @return 是否匹配
	 */
	public static boolean matchTime(RuleConditionDTO condition, Long matchTime) {
		boolean match = false;
		if (StringUtils.equals(condition.getOperator(), RecycleConditionTimeOperator.FIXED.name()) && StringUtils.isNotBlank(condition.getValue())) {
			// 固定时间
			String[] split = StringUtils.split(condition.getValue(), ",");
			if (split.length == FIX_TIME_LEN) {
				match = matchTime >= Long.parseLong(split[0]) && matchTime <= Long.parseLong(split[1]);
			}
		} else {
			// 动态时间
			LocalDateTime dynamicTime = condition.getDynamicTime();
			if (dynamicTime != null) {
				LocalDateTime pickedTime = Instant.ofEpochMilli(matchTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
				match = pickedTime.isBefore(dynamicTime);
			}
		}
		return match;
	}
}
