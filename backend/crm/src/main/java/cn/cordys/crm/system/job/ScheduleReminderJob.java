package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.message.TriggerTypeEnum;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.message.ScheduleTypeConfig;
import cn.cordys.crm.system.dto.message.TriggerTypeConfigDTO;
import cn.cordys.crm.system.job.schedule.ScheduleHit;
import cn.cordys.crm.system.job.schedule.ScheduleReminderResolver;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用「定时提醒」任务
 * <p>
 * 每小时执行，扫描所有 trigger_type = SCHEDULE 的消息配置，解析 value.scheduleTypeConfig，
 * 按「字段/提前天数/提醒时刻/提醒类型」在到期时间窗口内查出业务数据并发送通知。
 * 取数由 {@link ScheduleReminderResolver} 依据字段元数据通用解析，支持所有系统模块与自定义表单。
 * </p>
 */
@Component
@Slf4j
public class ScheduleReminderJob {

    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;
    @Resource
    private ScheduleReminderResolver scheduleReminderResolver;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    //@QuartzScheduled(cron = "0 0 * * * ?")

    //todo 测试11111111111111111111111111111
    @QuartzScheduled(cron = "0 * * * * ?")
    public void onEvent() {
        try {
            process();
        } catch (Exception e) {
            log.error("定时提醒任务异常: ", e);
        }
    }

    private void process() {
        List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                new LambdaQueryWrapper<MessageTaskConfig>()
                        .eq(MessageTaskConfig::getTriggerType, TriggerTypeEnum.SCHEDULE.name()));
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }
        List<String> taskIds = configs.stream()
                .map(MessageTaskConfig::getMessageTaskId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(taskIds)) {
            return;
        }
        Map<String, MessageTask> taskMap = messageTaskMapper.selectListByLambda(
                        new LambdaQueryWrapper<MessageTask>()
                                .in(MessageTask::getId, taskIds)
                                .eq(MessageTask::getEnable, true))
                .stream()
                .collect(Collectors.toMap(MessageTask::getId, Function.identity(), (a, b) -> a));

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        for (MessageTaskConfig config : configs) {
            try {
                MessageTask task = taskMap.get(config.getMessageTaskId());
                if (task == null) {
                    continue;
                }
                ScheduleTypeConfig schedule = parseSchedule(config.getValue());
                if (schedule == null) {
                    continue;
                }
                Integer dueHour = parseHour(schedule.getTime());
                if (dueHour == null || now.getHour() != dueHour) {
                    continue;
                }
                int day = parseDay(schedule.getDay());
                long[] window = buildWindow(now.toLocalDate(), day, isEveryDay(schedule.getRemindType()));
                List<ScheduleHit> hits = scheduleReminderResolver.findDue(task, schedule.getFieldId(),
                        window[0], window[1]);
                if (CollectionUtils.isEmpty(hits)) {
                    continue;
                }
                for (ScheduleHit hit : hits) {
                    try {
                        send(task, hit);
                    } catch (Exception e) {
                        log.error("定时提醒发送失败, taskId:{}, resourceId:{}", task.getId(), hit.id(), e);
                    }
                }
                log.info("定时提醒处理完成, event:{}, org:{}, count:{}", task.getEvent(),
                        task.getOrganizationId(), hits.size());
            } catch (Exception e) {
                log.error("处理定时提醒配置失败, configId:{}", config.getId(), e);
            }
        }
    }

    private void send(MessageTask task, ScheduleHit hit) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate dueDate = Instant.ofEpochMilli(hit.dueTime()).atZone(zone).toLocalDate();
        long expireDays = ChronoUnit.DAYS.between(today, dueDate);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("owner", hit.owner());
        newValues.put("name", hit.resourceName());
        newValues.put("id", hit.id());

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", hit.resourceName());
        paramMap.put("customerName", hit.resourceName());
        paramMap.put("expireDays", expireDays);
        paramMap.put("resourceId", hit.id());
        if (NotificationConstants.Event.FOLLOW_UP_PLAN_DUE.equals(task.getEvent())) {
            paramMap.put("field", "预计开始时间");
            paramMap.put("value", TimeUtils.getDateTimeStr(hit.dueTime()));
        }

        NoticeSendContext context = NoticeSendContext.builder()
                .taskType(task.getTaskType())
                .action(task.getEvent())
                .operatorId(hit.createUser())
                .organizationId(task.getOrganizationId())
                .resourceId(hit.id())
                .resourceName(hit.resourceName())
                .oldValues(Map.of())
                .newValues(newValues)
                .paramMap(paramMap)
                .excludeSelf(false)
                .build();
        commonNoticeSendService.sendScheduleNotice(task, context);
    }

    private ScheduleTypeConfig parseSchedule(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        TriggerTypeConfigDTO config = JSON.parseObject(value, TriggerTypeConfigDTO.class);
        return config == null ? null : config.getScheduleTypeConfig();
    }

    private Integer parseHour(String time) {
        if (StringUtils.isBlank(time)) {
            return null;
        }
        // time 形如 "08：00：00"（全角冒号）或 "08:00:00"
        String normalized = time.trim().replace('：', ':');
        try {
            return Integer.parseInt(normalized.split(":")[0].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseDay(String day) {
        if (StringUtils.isBlank(day)) {
            return 0;
        }
        try {
            return Integer.parseInt(day.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isEveryDay(String remindType) {
        return "EVERY_DAY".equalsIgnoreCase(remindType);
    }

    /**
     * 计算到期日期窗口 [start, end)：
     * ONLY_ONE —— due == today + day；
     * EVERY_DAY —— today <= due <= today + day。
     */
    private long[] buildWindow(LocalDate today, int day, boolean everyDay) {
        ZoneId zone = ZoneId.systemDefault();
        long end = today.plusDays(day + 1L).atStartOfDay(zone).toInstant().toEpochMilli();
        long start = everyDay
                ? today.atStartOfDay(zone).toInstant().toEpochMilli()
                : today.plusDays(day).atStartOfDay(zone).toInstant().toEpochMilli();
        return new long[]{start, end};
    }
}
