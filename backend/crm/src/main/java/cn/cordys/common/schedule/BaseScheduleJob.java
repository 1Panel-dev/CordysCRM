package cn.cordys.common.schedule;

import cn.cordys.common.util.LogUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

/**
 * 基础调度任务类，所有调度任务都应该继承此类，并实现具体的业务逻辑。
 * <p>
 * 本类提供了调度任务执行所需的资源信息，并通过抽象方法 {@link #businessExecute(JobExecutionContext)}
 * 让子类实现具体的业务逻辑。
 * </p>
 *
 * @since 1.0
 */
public abstract class BaseScheduleJob implements Job {

    /**
     * 资源 ID，表示该任务所关联的资源。
     */
    protected String resourceId;

    /**
     * 用户 ID，表示该任务执行的用户。
     */
    protected String userId;

    /**
     * 调度表达式，用于任务的调度规则。
     */
    protected String expression;

    /**
     * 执行调度任务时调用，提取任务所需的信息并调用子类的业务执行方法。
     *
     * @param context 任务执行的上下文对象
     */
    @Override
    public void execute(JobExecutionContext context) {
        // 从 JobDataMap 中获取任务所需的资源信息
        JobKey jobKey = context.getTrigger().getJobKey();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        this.resourceId = jobDataMap.getString("resourceId");
        this.userId = jobDataMap.getString("userId");
        this.expression = jobDataMap.getString("expression");

        // 记录日志，显示当前任务的执行情况
        LogUtils.info(jobKey.getGroup() + " Running: " + resourceId);

        // 调用子类实现的业务逻辑
        businessExecute(context);
    }

    /**
     * 子类需要实现该方法，定义任务执行的具体业务逻辑。
     *
     * @param context 任务执行的上下文对象
     */
    protected abstract void businessExecute(JobExecutionContext context);
}
