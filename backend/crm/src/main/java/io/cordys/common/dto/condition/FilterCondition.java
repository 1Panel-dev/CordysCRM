package io.cordys.common.dto.condition;

import io.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 表示组合条件，用于支持复杂的过滤和查询逻辑。
 * 包含字段名、操作符和期望值等信息。
 */
@Data
public class FilterCondition {

    /**
     * 系统字段为字段名
     * 模块字段为字段ID
     */
    @Schema(description = "条件的参数名称")
    @NotNull
    private String name;

    @Schema(description = "期望值，若操作符为 BETWEEN, IN, NOT_IN 时为数组，其他操作符为单个值")
    private Object value;

    @Schema(description = "是否是多选值")
    @NotNull
    private Boolean multipleValue = false;

    @Schema(description = "操作符",
            allowableValues = {"IN", "NOT_IN", "BETWEEN", "GT", "LT", "COUNT_GT", "COUNT_LT", "EQUALS", "NOT_EQUALS", "CONTAINS", "NOT_CONTAINS", "EMPTY", "NOT_EMPTY"})
    @EnumValue(enumClass = CombineConditionOperator.class)
    private String operator;

    @Schema(description = "类型")
    private String type;
    private List<Long> yesterdays;

    /**
     * 校验条件是否合法，检查字段名称、操作符和值的有效性。
     *
     * @return 如果条件合法则返回 true，否则返回 false
     */
    public boolean valid() {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(operator)) {
            return false;
        }

        // 针对空值判断操作符
        if (StringUtils.equalsAny(operator, CombineConditionOperator.EMPTY.name(), CombineConditionOperator.NOT_EMPTY.name())) {
            return true;
        }

        if (value == null) {
            return false;
        }

        // 针对值为集合类型的校验
        if (value instanceof List<?> valueList && CollectionUtils.isEmpty(valueList)) {
            return false;
        }

        // 针对值为字符串的校验
        return !(value instanceof String valueStr) || !StringUtils.isBlank(valueStr);
    }

    public boolean expectMulti() {
        return StringUtils.equalsAny(operator, CombineConditionOperator.IN.name(), CombineConditionOperator.NOT_IN.name(), CombineConditionOperator.BETWEEN.name(), CombineConditionOperator.DYNAMICS.name());
    }


    public Object getValue() {
        if (StringUtils.equalsIgnoreCase(operator, CombineConditionOperator.DYNAMICS.name()) ){
           // value 转为string 类型
            String strValue = (String) value;
            String[] split = strValue.split(",");
            if (split.length == 1) {
                String dateValue = split[0];
                switch(dateValue) {
                    case "TODAY" -> {
                        List<Long> todayList = new ArrayList<>();
                        // 获取今天的日期
                        LocalDate today = LocalDate.now();
                        long timestamp = getTimestamp(today.atStartOfDay());
                        todayList.add(timestamp);
                        long timestampEnd = getTimestamp(today.atTime(23, 59, 59, 999_000_000));
                        todayList.add(timestampEnd);
                        return todayList;
                    }
                    case "YESTERDAY" -> {
                        List<Long> yesterdayList = new ArrayList<>();
                        LocalDate yesterday = LocalDate.now().minusDays(1);
                        long timestamp = getTimestamp(yesterday.atStartOfDay());
                        yesterdayList.add(timestamp);
                        long timestampEnd = getTimestamp(yesterday.atTime(23, 59, 59, 999_000_000));
                        yesterdayList.add(timestampEnd);
                        return yesterdayList;
                    }
                    case "TOMORROW" -> {
                        List<Long> tomorrowList = new ArrayList<>();
                        LocalDate tomorrow = LocalDate.now().plusDays(1);
                        long timestamp = getTimestamp(tomorrow.atStartOfDay());
                        tomorrowList.add(timestamp);
                        long timestampEnd = getTimestamp(tomorrow.atTime(23, 59, 59, 999_000_000));
                        tomorrowList.add(timestampEnd);
                        return tomorrowList;
                    }
                    case "WEEK" -> {
                        List<Long> weeks = new ArrayList<>();
                        LocalDate startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                        long timestamp = getTimestamp(startOfWeek.atStartOfDay());
                        weeks.add(timestamp);
                        weeks.add(System.currentTimeMillis());
                        return weeks;
                    }
                    case "LAST_WEEK" -> {
                        List<Long> lastWeeks = new ArrayList<>();
                        LocalDate startOfLastWeek = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                        long timestamp = getTimestamp(startOfLastWeek.atStartOfDay());
                        lastWeeks.add(timestamp);
                        long timestampEnd = getTimestamp(startOfLastWeek.atTime(23, 59, 59, 999_000_000));
                        lastWeeks.add(timestampEnd);
                        return lastWeeks;
                    }
                    case "MONTH" -> {
                        List<Long> months = new ArrayList<>();
                        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfMonth.atStartOfDay());
                        months.add(timestamp);
                        months.add(System.currentTimeMillis());
                        return months;
                    }
                    case "LAST_MONTH" -> {
                        List<Long> lastMonths = new ArrayList<>();
                        LocalDate startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfLastMonth.atStartOfDay());
                        lastMonths.add(timestamp);
                        long timestampEnd = getTimestamp(startOfLastMonth.atTime(23, 59, 59, 999_000_000));
                        lastMonths.add(timestampEnd);
                        return lastMonths;
                    }
                    case "LAST_SEVEN" -> {
                        List<Long> lastSevens = new ArrayList<>();
                        LocalDate startOfLastSevenDays = LocalDate.now().minusDays(7);
                        long timestamp = getTimestamp(startOfLastSevenDays.atStartOfDay());
                        lastSevens.add(timestamp);
                        lastSevens.add(System.currentTimeMillis());
                        return lastSevens;
                    }
                    case "SEVEN" -> {
                        List<Long> sevens = new ArrayList<>();
                        LocalDate startOfNextSevenDays = LocalDate.now().plusDays(7);
                        long timestamp = getTimestamp(startOfNextSevenDays.atStartOfDay());
                        sevens.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextSevenDays.atTime(23, 59, 59, 999_000_000));
                        sevens.add(timestampEnd);
                        return sevens;
                    }
                    case "LAST_THIRTY" -> {
                        List<Long> lastThirty = new ArrayList<>();
                        LocalDate startOfLastThirtyDays = LocalDate.now().minusDays(30);
                        long timestamp = getTimestamp(startOfLastThirtyDays.atStartOfDay());
                        lastThirty.add(timestamp);
                        lastThirty.add(System.currentTimeMillis());
                        return lastThirty;
                    }
                    case "THIRTY" -> {
                        List<Long> thirty = new ArrayList<>();
                        LocalDate startOfNextThirtyDays = LocalDate.now().plusDays(30);
                        long timestamp = getTimestamp(startOfNextThirtyDays.atStartOfDay());
                        thirty.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextThirtyDays.atTime(23, 59, 59, 999_000_000));
                        thirty.add(timestampEnd);
                        return thirty;
                    }
                }

            } else{
                String dateValue = split[1];
                String dateUnit = split[2];
                int dateNumber = Integer.parseInt(dateValue);
                switch (dateUnit){
                    case "BEFORE_DAY"->{
                        List<Long> days = new ArrayList<>();
                        LocalDate startOfLastDays = LocalDate.now().minusDays(dateNumber);
                        long timestamp = getTimestamp(startOfLastDays.atStartOfDay());
                        days.add(timestamp);
                        long timestampEnd = getTimestamp(startOfLastDays.atTime(23, 59, 59, 999_000_000));
                        days.add(timestampEnd);
                        return days;
                    }
                    case "AFTER_DAY"->{
                        List<Long> days = new ArrayList<>();
                        LocalDate startOfNextDays = LocalDate.now().plusDays(dateNumber);
                        long timestamp = getTimestamp(startOfNextDays.atStartOfDay());
                        days.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextDays.atTime(23, 59, 59, 999_000_000));
                        days.add(timestampEnd);
                        return days;
                    }
                    case "BEFORE_WEEK"->{
                        List<Long> weeks = new ArrayList<>();
                        LocalDate startOfLastWeeks = LocalDate.now().minusWeeks(dateNumber).with(java.time.DayOfWeek.MONDAY);
                        long timestamp = getTimestamp(startOfLastWeeks.atStartOfDay());
                        weeks.add(timestamp);
                        long timestampEnd = getTimestamp(startOfLastWeeks.atTime(23, 59, 59, 999_000_000));
                        weeks.add(timestampEnd);
                        return weeks;
                    }
                    case "AFTER_WEEK"->{
                        List<Long> weeks = new ArrayList<>();
                        LocalDate startOfNextWeeks = LocalDate.now().plusWeeks(dateNumber).with(java.time.DayOfWeek.MONDAY);
                        long timestamp = getTimestamp(startOfNextWeeks.atStartOfDay());
                        weeks.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextWeeks.atTime(23, 59, 59, 999_000_000));
                        weeks.add(timestampEnd);
                        return weeks;
                    }
                    case "BEFORE_MONTH"->{
                        List<Long> months = new ArrayList<>();
                        LocalDate startOfLastMonths = LocalDate.now().minusMonths(dateNumber).withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfLastMonths.atStartOfDay());
                        months.add(timestamp);
                        long timestampEnd = getTimestamp(startOfLastMonths.atTime(23, 59, 59, 999_000_000));
                        months.add(timestampEnd);
                        return months;
                    }
                    case "AFTER_MONTH"->{
                        List<Long> months = new ArrayList<>();
                        LocalDate startOfNextMonths = LocalDate.now().plusMonths(dateNumber).withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfNextMonths.atStartOfDay());
                        months.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextMonths.atTime(23, 59, 59, 999_000_000));
                        months.add(timestampEnd);
                        return months;
                    }
                }
            }
        }
        return value;
    }

    private long getTimestamp(LocalDateTime today) {
        // 使用系统默认时区
        ZonedDateTime zonedEndOfDay = today.atZone(ZoneId.systemDefault());
        // 转为时间戳（毫秒）
        return zonedEndOfDay.toInstant().toEpochMilli();
    }

    /**
     * 枚举：组合条件操作符，定义了各种可能的查询操作符。
     */
    public enum CombineConditionOperator {
        /**
         * 动态
         */
        DYNAMICS,
        /**
         * 属于某个集合
         */
        IN,

        /**
         * 不属于某个集合
         */
        NOT_IN,

        /**
         * 区间操作
         */
        BETWEEN,

        /**
         * 大于
         */
        GT,

        /**
         * 小于
         */
        LT,

        /**
         * 数量大于
         */
        COUNT_GT,

        /**
         * 数量小于
         */
        COUNT_LT,

        /**
         * 等于
         */
        EQUALS,

        /**
         * 不等于
         */
        NOT_EQUALS,

        /**
         * 包含
         */
        CONTAINS,

        /**
         * 不包含
         */
        NOT_CONTAINS,

        /**
         * 为空
         */
        EMPTY,

        /**
         * 不为空
         */
        NOT_EMPTY
    }

    public enum CombineConditionDateValue{
        /**
         * 今天
         */
        TODAY,
        /** 昨天: */
        YESTERDAY,
        /** 明天：*/
        TOMORROW,
        /** 本周： */
        WEEK,
        /** 上周： */
        LAST_WEEK,
        /** 本月： */
        MONTH,
        /** 上月： */
        LAST_MONTH,
        /** 过去7天内： */
        LAST_SEVEN,
        /** 未来7天内： */
        SEVEN,
        /** 过去30天内： */
        LAST_THIRTY,
        /** 未来30天内： */
        THIRTY,
        /** 天前 */
        BEFORE_DAY,
        /** 天后 */
        AFTER_DAY,
        /** 周前 */
        BEFORE_WEEK,
        /** 周后 */
        AFTER_WEEK,
        /** 月前 */
        BEFORE_MONTH,
        /** 月后 */
        AFTER_MONTH,
    }
}
