package cn.cordys.crm.home.dto.request;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.crm.home.constants.HomeStatisticPeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticSearchWrapperRequest {

  private HomeStatisticSearchRequest staticRequest;
  private DeptDataPermissionDTO dataPermission;
  private String orgId;
  private String userId;

  public HomeStatisticSearchWrapperRequest(
      HomeStatisticSearchRequest staticRequest,
      DeptDataPermissionDTO dataPermission,
      String orgId,
      String userId) {
    this.staticRequest = staticRequest;
    this.dataPermission = dataPermission;
    this.orgId = orgId;
    this.userId = userId;
  }

  public boolean comparePeriod() {
    return StringUtils.isNotBlank(staticRequest.getPeriod())
        && BooleanUtils.isTrue(staticRequest.getPriorPeriodEnable());
  }

  public Long getStartTime() {
    String period = staticRequest.getPeriod();
    Long startTime = staticRequest.getStartTime();
    if (startTime != null) {
      return startTime;
    }
    if (StringUtils.isNotBlank(period)) {
      LocalDate now = LocalDate.now();
      HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
      startTime =
          switch (statisticPeriod) {
            case TODAY -> now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case THIS_WEEK ->
                now.with(DayOfWeek.MONDAY)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_MONTH ->
                now.withDayOfMonth(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_YEAR ->
                now.withDayOfYear(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
          };
    }
    return startTime;
  }

  public void setStartTime(Long startTime) {
    staticRequest.setStartTime(startTime);
  }

  public Long getPeriodStartTime() {
    String period = staticRequest.getPeriod();
    Long startTime = staticRequest.getStartTime();
    if (StringUtils.isNotBlank(period)) {
      LocalDate now = LocalDate.now();
      HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
      startTime =
          switch (statisticPeriod) {
            case TODAY ->
                now.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case THIS_WEEK ->
                now.minusWeeks(1)
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_MONTH ->
                now.minusMonths(1)
                    .withDayOfMonth(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_YEAR ->
                now.minusYears(1)
                    .withDayOfYear(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
          };
    }
    return startTime;
  }

  public Long getPeriodEndTime() {
    String period = staticRequest.getPeriod();
    Long endTime = staticRequest.getEndTime();
    if (StringUtils.isNotBlank(period)) {
      LocalDate now = LocalDate.now();
      HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
      endTime =
          switch (statisticPeriod) {
            case TODAY ->
                now.minusDays(1)
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_WEEK ->
                now.minusWeeks(1)
                    .with(DayOfWeek.SUNDAY)
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_MONTH -> {
              LocalDate startOfMonth = now.minusMonths(1).withDayOfMonth(1);
              yield startOfMonth
                  .withDayOfMonth(startOfMonth.lengthOfMonth())
                  .plusDays(1)
                  .minusDays(1)
                  .atTime(23, 59, 59)
                  .atZone(ZoneId.systemDefault())
                  .toInstant()
                  .toEpochMilli();
            }
            case THIS_YEAR ->
                now.minusYears(1)
                    .withDayOfYear(1)
                    .withDayOfYear(now.minusYears(1).lengthOfYear())
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
          };
    }
    return endTime;
  }

  public Long getEndTime() {
    String period = staticRequest.getPeriod();
    Long endTime = staticRequest.getEndTime();
    if (endTime != null) {
      return endTime;
    }
    if (StringUtils.isNotBlank(period)) {
      LocalDate now = LocalDate.now();
      HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
      endTime =
          switch (statisticPeriod) {
            case TODAY ->
                now.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case THIS_WEEK ->
                now.with(DayOfWeek.SUNDAY)
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            case THIS_MONTH -> {
              LocalDate startOfMonth = now.withDayOfMonth(1);
              yield startOfMonth
                  .withDayOfMonth(startOfMonth.lengthOfMonth())
                  .plusDays(1)
                  .minusDays(1)
                  .atTime(23, 59, 59)
                  .atZone(ZoneId.systemDefault())
                  .toInstant()
                  .toEpochMilli();
            }
            case THIS_YEAR ->
                now.withDayOfYear(1)
                    .withDayOfYear(now.lengthOfYear())
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
          };
    }
    return endTime;
  }

  public void setEndTime(Long endTime) {
    staticRequest.setEndTime(endTime);
  }
}
