package cn.cordys.crm.system.dto;

import cn.cordys.crm.system.constants.RecycleConditionOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Strings;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
     *
     * @return 动态时间
     */
    public LocalDateTime getDynamicTime() {
        if (!Strings.CS.equals(this.operator, RecycleConditionOperator.DYNAMICS.name())) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        String[] timeArr = this.value.split(",");
        //兼容旧数据的计算
        if (timeArr.length == 2) {
            return switch (timeArr[1]) {
                case "month" -> now.minusMonths(Long.parseLong(timeArr[0]));
                case "week" -> now.minusWeeks(Long.parseLong(timeArr[0]));
                case "day" -> now.minusDays(Long.parseLong(timeArr[0]));
                default -> null;
            };
        } else if (timeArr.length == 3) {
            String dateValue = timeArr[1];
            String dateUnit = timeArr[2];
            long dateNumber = Long.parseLong(dateValue);
            return switch (dateUnit) {
                case "BEFORE_DAY" -> now.minusDays(dateNumber);
                case "AFTER_DAY" -> now.plusDays(dateNumber);
                case "BEFORE_WEEK" -> now.minusWeeks(dateNumber);
                case "AFTER_WEEK" -> now.plusWeeks(dateNumber);
                case "BEFORE_MONTH" -> now.minusMonths(dateNumber);
                case "AFTER_MONTH" -> now.plusMonths(dateNumber);
                default -> null;
            };
        }
        return null;
    }


    public List<Long> getDynamicTimeList() {
        if (!Strings.CS.equals(this.operator, RecycleConditionOperator.DYNAMICS.name())) {
            return null;
        }
        List<Long> timestamps = new ArrayList<>();
        String[] timeArr = this.value.split(",");
        //TODO：兼容旧数据的计算
        if (timeArr.length == 1) {
            switch (timeArr[0]) {
                case "TODAY" -> {
                    // 获取今天的日期
                    LocalDate today = LocalDate.now();
                    long timestamp = getTimestamp(today.atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(today.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "YESTERDAY" -> {
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    long timestamp = getTimestamp(yesterday.atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(yesterday.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "TOMORROW" -> {
                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                    long timestamp = getTimestamp(tomorrow.atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(tomorrow.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "WEEK" -> {
                    LocalDate startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                    long timestamp = getTimestamp(startOfWeek.atStartOfDay());
                    timestamps.add(timestamp);
                    LocalDate now = LocalDate.now().with(DayOfWeek.SUNDAY);
                    ;
                    long timestampEnd = getTimestamp(now.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "LAST_WEEK" -> {
                    LocalDate startOfLastWeek = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                    long timestamp = getTimestamp(startOfLastWeek.atStartOfDay());
                    timestamps.add(timestamp);
                    LocalDate startOfLastWeekEnd = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);
                    long timestampEnd = getTimestamp(startOfLastWeekEnd.plusDays(7).atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "MONTH" -> {
                    LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                    long timestamp = getTimestamp(startOfMonth.atStartOfDay());
                    timestamps.add(timestamp);
                    LocalDate now = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
                    long timestampEnd = getTimestamp(now.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "LAST_MONTH" -> {
                    LocalDate startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                    long timestamp = getTimestamp(startOfLastMonth.atStartOfDay());
                    timestamps.add(timestamp);
                    LocalDate startOfLastMonthEnd = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    long timestampEnd = getTimestamp(startOfLastMonthEnd.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "LAST_SEVEN" -> {
                    LocalDate startOfLastSevenDays = LocalDate.now().minusDays(7);
                    long timestamp = getTimestamp(startOfLastSevenDays.atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(LocalDate.now().atStartOfDay());
                    timestamps.add(timestampEnd);
                }
                case "SEVEN" -> {
                    LocalDate startOfNextSevenDays = LocalDate.now().plusDays(6);
                    long timestamp = getTimestamp(LocalDate.now().atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(startOfNextSevenDays.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
                case "LAST_THIRTY" -> {
                    LocalDate startOfLastThirtyDays = LocalDate.now().minusDays(30);
                    long timestamp = getTimestamp(startOfLastThirtyDays.atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(LocalDate.now().atStartOfDay());
                    timestamps.add(timestampEnd);
                }
                case "THIRTY" -> {
                    LocalDate startOfNextThirtyDays = LocalDate.now().plusDays(29);
                    long timestamp = getTimestamp(LocalDate.now().atStartOfDay());
                    timestamps.add(timestamp);
                    long timestampEnd = getTimestamp(startOfNextThirtyDays.atTime(23, 59, 59, 999_000_000));
                    timestamps.add(timestampEnd);
                }
            }
            ;
        }
        return timestamps;
    }

    private long getTimestamp(LocalDateTime today) {
        // 使用系统默认时区
        ZonedDateTime zonedEndOfDay = today.atZone(ZoneId.systemDefault());
        // 转为时间戳（毫秒）
        return zonedEndOfDay.toInstant().toEpochMilli();
    }

}
