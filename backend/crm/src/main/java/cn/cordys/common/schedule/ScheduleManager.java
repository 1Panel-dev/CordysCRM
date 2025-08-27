package cn.cordys.common.schedule;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.domain.Schedule;
import jakarta.annotation.Resource;
import org.quartz.*;

/**
 * 定时任务管理器，用于管理调度任务的添加、修改、删除等操作。
 * 提供了对简单任务和 Cron 表达式任务的支持。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>添加和删除定时任务</li>
 *   <li>修改 Cron 表达式</li>
 *   <li>启动和关闭调度器</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
public class ScheduleManager {

    @Resource
    private Scheduler scheduler;

    /**
     * 添加一个简单的定时任务。
     *
     * @param jobKey       任务标识
     * @param triggerKey   触发器标识
     * @param cls          任务类
     * @param repeatIntervalTime 任务重复间隔时间（单位：小时）
     * @param jobDataMap   任务数据
     * @throws SchedulerException 如果调度失败
     */
    public void addSimpleJob(JobKey jobKey, TriggerKey triggerKey, Class<? extends Job> cls, int repeatIntervalTime, JobDataMap jobDataMap)
            throws SchedulerException {

        JobBuilder jobBuilder = JobBuilder.newJob(cls).withIdentity(jobKey);
        if (jobDataMap != null) {
            jobBuilder.usingJobData(jobDataMap);
        }

        SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(repeatIntervalTime).repeatForever())
                .startNow().build();

        scheduler.scheduleJob(jobBuilder.build(), trigger);
    }

    /**
     * 添加一个 Cron 表达式定时任务。
     *
     * @param jobKey       任务标识
     * @param triggerKey   触发器标识
     * @param jobClass     任务类
     * @param cron         Cron 表达式
     * @param jobDataMap   任务数据
     */
    public void addCronJob(JobKey jobKey, TriggerKey triggerKey, Class<? extends Job> jobClass, String cron, JobDataMap jobDataMap) {
        try {
            LogUtils.info("addCronJob: " + triggerKey.getName() + "," + triggerKey.getGroup());
            JobBuilder jobBuilder = JobBuilder.newJob(jobClass).withIdentity(jobKey);
            if (jobDataMap != null) {
                jobBuilder.usingJobData(jobDataMap);
            }

            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
            triggerBuilder.withIdentity(triggerKey);
            triggerBuilder.startNow();
            triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
            CronTrigger trigger = (CronTrigger) triggerBuilder.build();
            scheduler.scheduleJob(jobBuilder.build(), trigger);

        } catch (Exception e) {
            LogUtils.error(e);
            throw new GenericException("定时任务配置异常: " + e.getMessage(), e);
        }
    }

    /**
     * 添加一个 Cron 表达式定时任务（不带 JobDataMap）。
     *
     * @param jobKey     任务标识
     * @param triggerKey 触发器标识
     * @param jobClass   任务类
     * @param cron       Cron 表达式
     */
    public void addCronJob(JobKey jobKey, TriggerKey triggerKey, Class<? extends Job> jobClass, String cron) {
        addCronJob(jobKey, triggerKey, jobClass, cron, null);
    }

    /**
     * 修改现有的 Cron 触发器的 Cron 表达式。
     *
     * @param triggerKey 触发器标识
     * @param cron       新的 Cron 表达式
     * @throws SchedulerException 如果修改失败
     */
    public void modifyCronJobTime(TriggerKey triggerKey, String cron) throws SchedulerException {

        LogUtils.info("modifyCronJobTime: " + triggerKey.getName() + "," + triggerKey.getGroup());
        try {
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }

            String oldTime = trigger.getCronExpression();
            if (!oldTime.equalsIgnoreCase(cron)) {
                // 修改触发器的 Cron 表达式
                TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
                triggerBuilder.withIdentity(triggerKey);
                triggerBuilder.startNow();
                triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
                trigger = (CronTrigger) triggerBuilder.build();
                scheduler.rescheduleJob(triggerKey, trigger);
            }
        } catch (Exception e) {
            throw new RuntimeException("修改 Cron 表达式失败", e);
        }
    }

    /**
     * 删除指定的任务和触发器。
     *
     * @param jobKey     任务标识
     * @param triggerKey 触发器标识
     */
    public void removeJob(JobKey jobKey, TriggerKey triggerKey) {
        try {
            LogUtils.info("RemoveJob: " + jobKey.getName() + "," + jobKey.getGroup());
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LogUtils.error("删除任务失败", e);
            throw new RuntimeException("删除任务失败", e);
        }
    }

    /**
     * 启动调度器。
     *
     * @param schedule 调度器
     */
    public static void startJobs(Scheduler schedule) {
        try {
            schedule.start();
        } catch (Exception e) {
            LogUtils.error("启动调度器失败", e);
            throw new RuntimeException("启动调度器失败", e);
        }
    }

    /**
     * 关闭调度器。
     *
     * @param schedule 调度器
     */
    public void shutdownJobs(Scheduler schedule) {
        try {
            if (!schedule.isShutdown()) {
                schedule.shutdown();
            }
        } catch (Exception e) {
            LogUtils.error("关闭调度器失败", e);
            throw new RuntimeException("关闭调度器失败", e);
        }
    }

    /**
     * 添加或更新 Cron 定时任务。
     * 如果触发器已存在，则修改其 Cron 表达式；否则，添加新的 Cron 定时任务。
     *
     * @param jobKey     任务标识
     * @param triggerKey 触发器标识
     * @param jobClass   任务类
     * @param cron       Cron 表达式
     * @param jobDataMap 任务数据
     * @throws SchedulerException 如果添加或更新任务失败
     */
    public void addOrUpdateCronJob(JobKey jobKey, TriggerKey triggerKey, Class jobClass, String cron, JobDataMap jobDataMap)
            throws SchedulerException {
        LogUtils.info("AddOrUpdateCronJob: " + jobKey.getName() + "," + triggerKey.getGroup());

        if (scheduler.checkExists(triggerKey)) {
            modifyCronJobTime(triggerKey, cron);
        } else {
            addCronJob(jobKey, triggerKey, jobClass, cron, jobDataMap);
        }
    }

    /**
     * 添加或更新 Cron 定时任务（不带 JobDataMap）。
     *
     * @param jobKey     任务标识
     * @param triggerKey 触发器标识
     * @param jobClass   任务类
     * @param cron       Cron 表达式
     * @throws SchedulerException 如果添加或更新任务失败
     */
    public void addOrUpdateCronJob(JobKey jobKey, TriggerKey triggerKey, Class jobClass, String cron) throws SchedulerException {
        addOrUpdateCronJob(jobKey, triggerKey, jobClass, cron, null);
    }

    /**
     * 获取默认的 JobDataMap，包含定时任务所需的基本信息。
     *
     * @param schedule 定时任务调度对象
     * @param expression Cron 或时间表达式
     * @param userId    执行任务的用户 ID
     * @return JobDataMap 对象
     */
    public JobDataMap getDefaultJobDataMap(Schedule schedule, String expression, String userId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("resourceId", schedule.getResourceId());
        jobDataMap.put("expression", expression);
        jobDataMap.put("userId", userId);
        jobDataMap.put("config", schedule.getConfig());
        jobDataMap.put("organizationId", schedule.getOrganizationId());
        return jobDataMap;
    }
}
