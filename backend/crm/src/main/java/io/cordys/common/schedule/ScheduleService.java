package io.cordys.common.schedule;

import io.cordys.common.constants.ApplicationNumScope;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.uid.NumGenerator;
import io.cordys.crm.system.domain.Schedule;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定时任务服务类，负责调度任务的创建、编辑、删除及相关操作。
 * 主要功能包括定时任务的增、删、查及其与调度器的交互。
 *
 * @since 1.0
 */
@Transactional(rollbackFor = Exception.class)
public class ScheduleService {

    @Resource
    private BaseMapper<Schedule> scheduleMapper;

    @Resource
    private ScheduleManager scheduleManager;

    /**
     * 添加新的定时任务。
     *
     * @param schedule 定时任务对象
     */
    public void addSchedule(Schedule schedule) {
        schedule.setId(IDGenerator.nextStr());
        schedule.setCreateTime(System.currentTimeMillis());
        schedule.setUpdateTime(System.currentTimeMillis());
        schedule.setNum(getNextNum(schedule.getOrganizationId()));
        scheduleMapper.insert(schedule);
    }

    /**
     * 获取下一个任务编号。
     *
     * @param organizationId 项目 ID
     * @return 下一个任务编号
     */
    public long getNextNum(String organizationId) {
        return NumGenerator.nextNum(organizationId, ApplicationNumScope.TASK);
    }

    /**
     * 根据任务 ID 获取定时任务。
     *
     * @param scheduleId 定时任务 ID
     * @return 定时任务对象
     */
    public Schedule getSchedule(String scheduleId) {
        return scheduleMapper.selectByPrimaryKey(scheduleId);
    }

    /**
     * 编辑定时任务信息。
     *
     * @param schedule 要更新的定时任务对象
     * @return 更新的记录数
     */
    public int editSchedule(Schedule schedule) {
        schedule.setUpdateTime(System.currentTimeMillis());
        return scheduleMapper.update(schedule);
    }

    /**
     * 根据任务标识删除调度器中的任务。
     *
     * @param key 任务标识
     * @param job 任务名
     */
    private void removeJob(String key, String job) {
        scheduleManager.removeJob(new JobKey(key, job), new TriggerKey(key, job));
    }

    /**
     * 添加或更新 Cron 表达式定时任务。
     * 如果定时任务启用且 Cron 表达式有效，则添加或更新任务；否则，移除任务。
     *
     * @param request    定时任务请求对象
     * @param jobKey     任务标识
     * @param triggerKey 触发器标识
     * @param clazz      任务类
     */
    public void addOrUpdateCronJob(Schedule request, JobKey jobKey, TriggerKey triggerKey, Class clazz) {
        Boolean enable = request.getEnable();
        String cronExpression = request.getValue();
        if (BooleanUtils.isTrue(enable) && StringUtils.isNotBlank(cronExpression)) {
            try {
                // 添加或更新 Cron 表达式定时任务
                scheduleManager.addOrUpdateCronJob(jobKey, triggerKey, clazz, cronExpression,
                        scheduleManager.getDefaultJobDataMap(request, cronExpression, request.getCreateUser()));
            } catch (SchedulerException e) {
                throw new GenericException("定时任务开启异常: " + e.getMessage());
            }
        } else {
            try {
                // 移除定时任务
                scheduleManager.removeJob(jobKey, triggerKey);
            } catch (Exception e) {
                throw new GenericException("定时任务关闭异常: " + e.getMessage());
            }
        }
    }
}
