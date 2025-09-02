package cn.cordys.common.dto.condition;

import cn.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
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
            allowableValues = {"IN", "NOT_IN", "BETWEEN", "GT", "LT", "GE", "LE", "COUNT_GT", "COUNT_LT", "EQUALS", "NOT_EQUALS", "CONTAINS", "NOT_CONTAINS", "EMPTY", "NOT_EMPTY"})
    @EnumValue(enumClass = CombineConditionOperator.class)
    private String operator;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "包含新增子部门集合")
    private List<String> containChildIds;

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
        if (Strings.CS.equalsAny(operator, CombineConditionOperator.EMPTY.name(), CombineConditionOperator.NOT_EMPTY.name())) {
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
        return Strings.CS.equalsAny(operator, CombineConditionOperator.IN.name(), CombineConditionOperator.NOT_IN.name(), CombineConditionOperator.BETWEEN.name(), CombineConditionOperator.DYNAMICS.name());
    }


    public Object getCombineValue() {
        if (Strings.CI.equals(operator, CombineConditionOperator.DYNAMICS.name())) {
            // value 转为string 类型
            String strValue = (String) value;
            String[] split = strValue.split(",");
            if (split.length == 1) {
                String dateValue = split[0];
                switch (dateValue) {
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
                        LocalDate now = LocalDate.now().with(DayOfWeek.SUNDAY);;
                        long timestampEnd = getTimestamp(now.atTime(23, 59, 59, 999_000_000));
                        weeks.add(timestampEnd);
                        return weeks;
                    }
                    case "LAST_WEEK" -> {
                        List<Long> lastWeeks = new ArrayList<>();
                        LocalDate startOfLastWeek = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                        long timestamp = getTimestamp(startOfLastWeek.atStartOfDay());
                        lastWeeks.add(timestamp);
                        LocalDate startOfLastWeekEnd = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);
                        long timestampEnd = getTimestamp(startOfLastWeekEnd.atTime(23, 59, 59, 999_000_000));
                        lastWeeks.add(timestampEnd);
                        return lastWeeks;
                    }
                    case "MONTH" -> {
                        List<Long> months = new ArrayList<>();
                        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfMonth.atStartOfDay());
                        months.add(timestamp);
                        LocalDate now = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
                        long timestampEnd = getTimestamp(now.atTime(23, 59, 59, 999_000_000));
                        months.add(timestampEnd);
                        return months;
                    }
                    case "LAST_MONTH" -> {
                        List<Long> lastMonths = new ArrayList<>();
                        LocalDate startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                        long timestamp = getTimestamp(startOfLastMonth.atStartOfDay());
                        lastMonths.add(timestamp);
                        LocalDate startOfLastMonthEnd = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                        long timestampEnd = getTimestamp(startOfLastMonthEnd.atTime(23, 59, 59, 999_000_000));
                        lastMonths.add(timestampEnd);
                        return lastMonths;
                    }
                    case "LAST_SEVEN" -> {
                        List<Long> lastSevens = new ArrayList<>();
                        LocalDate startOfLastSevenDays = LocalDate.now().minusDays(7);
                        long timestamp = getTimestamp(startOfLastSevenDays.atStartOfDay());
                        lastSevens.add(timestamp);
                        long timestampEnd = getTimestamp(LocalDate.now().atStartOfDay());
                        lastSevens.add(timestampEnd);
                        return lastSevens;
                    }
                    case "SEVEN" -> {
                        List<Long> sevens = new ArrayList<>();
                        LocalDate startOfNextSevenDays = LocalDate.now().plusDays(6);
                        long timestamp = getTimestamp(LocalDate.now().atStartOfDay());
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
                        long timestampEnd = getTimestamp(LocalDate.now().atStartOfDay());
                        lastThirty.add(timestampEnd);
                        return lastThirty;
                    }
                    case "THIRTY" -> {
                        List<Long> thirty = new ArrayList<>();
                        LocalDate startOfNextThirtyDays = LocalDate.now().plusDays(29);
                        long timestamp = getTimestamp(LocalDate.now().atStartOfDay());
                        thirty.add(timestamp);
                        long timestampEnd = getTimestamp(startOfNextThirtyDays.atTime(23, 59, 59, 999_000_000));
                        thirty.add(timestampEnd);
                        return thirty;
                    }
                }

            } else {
                String dateValue = split[1];
                String dateUnit = split[2];
                int dateNumber = Integer.parseInt(dateValue);
                switch (dateUnit) {
                    case "BEFORE_DAY" -> {
                        LocalDateTime startOfLastDays = LocalDateTime.now().minusDays(dateNumber);
                        return getTimestamp(startOfLastDays);
                    }
                    case "AFTER_DAY" -> {
                        LocalDateTime startOfNextDays = LocalDateTime.now().plusDays(dateNumber);
                        return getTimestamp(startOfNextDays);
                    }
                    case "BEFORE_WEEK" -> {
                        LocalDateTime startOfLastWeeks = LocalDateTime.now().minusDays(dateNumber * 7L);
                        return getTimestamp(startOfLastWeeks);
                    }
                    case "AFTER_WEEK" -> {
                        LocalDateTime startOfNextWeeks = LocalDateTime.now().plusDays(dateNumber * 7L);
                        return getTimestamp(startOfNextWeeks);
                    }
                    case "BEFORE_MONTH" -> {
                        LocalDateTime startOfLastMonths = LocalDateTime.now().minusMonths(dateNumber);
                        return getTimestamp(startOfLastMonths);
                    }
                    case "AFTER_MONTH" -> {
                        LocalDateTime startOfNextMonths = LocalDateTime.now().plusMonths(dateNumber);
                        return getTimestamp(startOfNextMonths);
                    }
                }
            }
        }
        return value;
    }

    public String getCombineOperator() {
        if (Strings.CI.equals(operator, CombineConditionOperator.DYNAMICS.name())) {
            String strValue = (String) value;
            String[] split = strValue.split(",");
            if (split.length == 1) {
                return CombineConditionOperator.BETWEEN.name();
            } else {
                String dateUnit = split[2];
                switch (dateUnit) {
                    case "BEFORE_DAY", "BEFORE_WEEK", "BEFORE_MONTH" -> {
                        return CombineConditionOperator.LT.name();
                    }
                    case "AFTER_DAY", "AFTER_WEEK", "AFTER_MONTH" -> {
                        return CombineConditionOperator.GT.name();
                    }
                }
            }

        }
        return operator;
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
         * 大于等于
         */
        GE,

        /**
         * 小于等于
         */
        LE,

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

}
