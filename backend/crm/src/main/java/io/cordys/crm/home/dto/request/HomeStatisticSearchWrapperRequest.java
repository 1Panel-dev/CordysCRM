package io.cordys.crm.home.dto.request;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.uid.utils.EnumUtils;
import io.cordys.crm.home.constants.HomeStatisticPeriod;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticSearchWrapperRequest {

    private HomeStatisticSearchRequest staticRequest;
    private DeptDataPermissionDTO dataPermission;
    private String orgId;
    private String userId;

    public HomeStatisticSearchWrapperRequest(HomeStatisticSearchRequest staticRequest,
                                             DeptDataPermissionDTO dataPermission,
                                             String orgId, String userId) {
        this.staticRequest = staticRequest;
        this.dataPermission = dataPermission;
        this.orgId = orgId;
        this.userId = userId;
    }

    public boolean comparePeriod() {
        return StringUtils.isNotBlank(staticRequest.getPeriod());
    }

    public void setStartTime(Long startTime) {
        staticRequest.setStartTime(startTime);
    }

    public void setEndTime(Long endTime) {
        staticRequest.setEndTime(endTime);
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
            switch (statisticPeriod) {
                case TODAY:
                    startTime = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_WEEK:
                    startTime = now.with(DayOfWeek.MONDAY)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_MONTH:
                    startTime = now.withDayOfMonth(1)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
            }
        }
        return startTime;
    }

    public Long getPeriodStartTime() {
        String period = staticRequest.getPeriod();
        Long startTime = staticRequest.getStartTime();
        if (StringUtils.isNotBlank(period)) {
            LocalDate now = LocalDate.now();
            HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
            switch (statisticPeriod) {
                case TODAY:
                    startTime = now.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_WEEK:
                    startTime = now.minusWeeks(1).with(DayOfWeek.MONDAY)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_MONTH:
                    startTime = now.minusMonths(1).withDayOfMonth(1)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
            }
        }
        return startTime;
    }

    public Long getPeriodEndTime() {
        String period = staticRequest.getPeriod();
        Long endTime = staticRequest.getEndTime();
        if (StringUtils.isNotBlank(period)) {
            LocalDate now = LocalDate.now();
            HomeStatisticPeriod statisticPeriod = EnumUtils.valueOf(HomeStatisticPeriod.class, period);
            switch (statisticPeriod) {
                case TODAY:
                    endTime = now.minusDays(1).atTime(23, 59, 59)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_WEEK:
                    endTime = now.minusWeeks(1).with(DayOfWeek.SUNDAY)
                            .atTime(23, 59, 59)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_MONTH:
                    LocalDate startOfMonth = now.minusMonths(1).withDayOfMonth(1);
                    LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth()).plusDays(1).minusDays(1);
                    endTime = endOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
            }
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
            switch (statisticPeriod) {
                case TODAY:
                    endTime = now.atTime(23, 59, 59)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_WEEK:
                    endTime = now.with(DayOfWeek.SUNDAY)
                            .atTime(23, 59, 59)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
                case THIS_MONTH:
                    LocalDate startOfMonth = now.withDayOfMonth(1);
                    LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth()).plusDays(1).minusDays(1);
                    endTime = endOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    break;
            }
        }
        return endTime;
    }
}
