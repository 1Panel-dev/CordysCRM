package io.cordys.common.util;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;


/**
 * 使用 Apache Commons Lang DateUtils 计算各种时间周期的日期范围的工具类。
 * <p>
 * 此类提供静态方法来获取常用时间周期的日期范围，如今天、本周、上月等。
 * </p>
 **/
public class DateRangeUtils {

    private DateRangeUtils() {
        // 私有构造函数防止实例化
    }

    /**
     * 返回给定时间周期的开始和结束日期。
     * <p>
     * 对于 CUSTOM 周期，必须提供 customStart 和 customEnd。
     * 对于其他周期，customStart 和 customEnd 参数将被忽略。
     * </p>
     *
     * @param period      要计算日期范围的时间周期
     * @param customStart 自定义开始日期（仅用于 CUSTOM 周期）
     * @param customEnd   自定义结束日期（仅用于 CUSTOM 周期）
     * @return 表示所请求时间周期的日期范围
     * @throws IllegalArgumentException      如果 CUSTOM 周期没有提供 customStart 和 customEnd
     * @throws UnsupportedOperationException 如果提供了不支持的时间周期
     */
    public static DateRange getRange(TimePeriod period, Date customStart, Date customEnd) {
        // 当前时间
        Date now = new Date();

        switch (period) {
            case TODAY:
                Date startToday = DateUtils.truncate(now, Calendar.DAY_OF_MONTH);
                return new DateRange(startToday, now);

            case YESTERDAY: {
                Date yesterday = DateUtils.addDays(now, -1);
                Date startY = DateUtils.truncate(yesterday, Calendar.DAY_OF_MONTH);
                Date endY = setEndOfDay(startY);
                return new DateRange(startY, endY);
            }

            case TOMORROW: {
                Date tomorrow = DateUtils.addDays(now, 1);
                Date startT = DateUtils.truncate(tomorrow, Calendar.DAY_OF_MONTH);
                Date endT = setEndOfDay(startT);
                return new DateRange(startT, endT);
            }

            case THIS_WEEK: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                // 设置第一天为星期一
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                // 移动到本周第一天
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                Date weekStart = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                return new DateRange(weekStart, now);
            }

            case LAST_WEEK: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                // 移动到本周一，然后回退 7 天
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                Date startLW = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                cal.add(Calendar.DAY_OF_WEEK, 6);
                Date endLW = setEndOfDay(cal.getTime());
                return new DateRange(startLW, endLW);
            }

            case THIS_MONTH: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date monthStart = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                return new DateRange(monthStart, now);
            }

            case LAST_MONTH: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.add(Calendar.MONTH, -1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date startLM = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                Date endLM = setEndOfDay(cal.getTime());
                return new DateRange(startLM, endLM);
            }

            case PAST_7_DAYS: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.add(Calendar.DAY_OF_YEAR, -7);
                Date start7 = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                return new DateRange(start7, now);
            }

            case NEXT_7_DAYS: {
                Date start = DateUtils.truncate(now, Calendar.DAY_OF_MONTH);
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);
                cal.add(Calendar.DAY_OF_YEAR, 7);
                Date end = setEndOfDay(cal.getTime());
                return new DateRange(start, end);
            }

            case PAST_30_DAYS: {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.add(Calendar.DAY_OF_YEAR, -30);
                Date start30 = DateUtils.truncate(cal.getTime(), Calendar.DAY_OF_MONTH);
                return new DateRange(start30, now);
            }

            case NEXT_30_DAYS: {
                Date startN30 = DateUtils.truncate(now, Calendar.DAY_OF_MONTH);
                Calendar cal = Calendar.getInstance();
                cal.setTime(startN30);
                cal.add(Calendar.DAY_OF_YEAR, 30);
                Date end30 = setEndOfDay(cal.getTime());
                return new DateRange(startN30, end30);
            }

            case CUSTOM:
                if (customStart == null || customEnd == null) {
                    throw new IllegalArgumentException("自定义时间周期必须提供开始和结束日期");
                }
                return new DateRange(customStart, customEnd);

            default:
                throw new UnsupportedOperationException("不支持的时间周期: " + period);
        }
    }

    /**
     * 返回给定时间周期的日期范围（不需要自定义日期的简化版本）。
     *
     * @param period 要计算日期范围的时间周期
     * @return 表示所请求时间周期的日期范围
     * @throws IllegalArgumentException      如果提供了 CUSTOM 周期
     * @throws UnsupportedOperationException 如果提供了不支持的时间周期
     */
    public static DateRange getRange(TimePeriod period) {
        if (period == TimePeriod.CUSTOM) {
            throw new IllegalArgumentException("使用 CUSTOM 时间周期时必须提供自定义开始和结束日期");
        }
        return getRange(period, null, null);
    }

    /**
     * 将给定日期设置为当天的最后一毫秒 (23:59:59.999)。
     *
     * @param date 要设置的日期
     * @return 设置为当天最后一毫秒的新日期
     */
    private static Date setEndOfDay(Date date) {
        Date result = DateUtils.setHours(date, 23);
        result = DateUtils.setMinutes(result, 59);
        result = DateUtils.setSeconds(result, 59);
        return DateUtils.addMilliseconds(result, 999);
    }

}

/**
 * 表示具有开始和结束日期的时间范围。
 * <p>
 * 该类是不可变的，创建后不能修改其开始或结束日期。
 * </p>
 */
class DateRange {
    private final Date start;
    private final Date end;

    /**
     * 创建一个新的日期范围。
     *
     * @param start 范围的开始日期，不能为 null
     * @param end   范围的结束日期，不能为 null
     * @throws IllegalArgumentException 如果 start 或 end 为 null
     */
    public DateRange(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("开始和结束日期不能为 null");
        }
        this.start = new Date(start.getTime());
        this.end = new Date(end.getTime());
    }

    /**
     * 获取范围的开始日期。
     *
     * @return 范围的开始日期（返回副本以维持不可变性）
     */
    public Date getStart() {
        return new Date(start.getTime());
    }

    /**
     * 获取范围的结束日期。
     *
     * @return 范围的结束日期（返回副本以维持不可变性）
     */
    public Date getEnd() {
        return new Date(end.getTime());
    }

    /**
     * 检查给定的日期是否在此范围内（包括开始和结束日期）。
     *
     * @param date 要检查的日期
     * @return 如果日期在此范围内则返回 true
     */
    public boolean contains(Date date) {
        if (date == null) {
            return false;
        }
        return !date.before(start) && !date.after(end);
    }
}

/**
 * 可选时间周期的枚举。
 * <p>
 * 这些预定义的时间周期可与 {@link DateRangeUtils} 一起使用，
 * 以获取常用时间范围。
 * </p>
 */
enum TimePeriod {
    /**
     * 今天（从当天 00:00:00 到当前时间）
     */
    TODAY,
    /**
     * 昨天（整天）
     */
    YESTERDAY,
    /**
     * 明天（整天）
     */
    TOMORROW,
    /**
     * 本周（从本周一 00:00:00 到当前时间）
     */
    THIS_WEEK,
    /**
     * 上周（从上周一 00:00:00 到上周日 23:59:59.999）
     */
    LAST_WEEK,
    /**
     * 本月（从本月 1 日 00:00:00 到当前时间）
     */
    THIS_MONTH,
    /**
     * 上月（从上月 1 日 00:00:00 到上月最后一天 23:59:59.999）
     */
    LAST_MONTH,
    /**
     * 过去 7 天（从 7 天前 00:00:00 到当前时间）
     */
    PAST_7_DAYS,
    /**
     * 未来 7 天（从今天 00:00:00 到 7 天后 23:59:59.999）
     */
    NEXT_7_DAYS,
    /**
     * 过去 30 天（从 30 天前 00:00:00 到当前时间）
     */
    PAST_30_DAYS,
    /**
     * 未来 30 天（从今天 00:00:00 到 30 天后 23:59:59.999）
     */
    NEXT_30_DAYS,
    /**
     * 自定义时间范围（需要提供自定义开始和结束日期）
     */
    CUSTOM
}
