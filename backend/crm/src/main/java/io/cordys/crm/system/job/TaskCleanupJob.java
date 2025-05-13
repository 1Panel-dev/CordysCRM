package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.job.listener.ExecuteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class TaskCleanupJob {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public TaskCleanupJob(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 系统定时任务执行
     * <p>
     * 该方法每晚 3 点执行，清理过期的任务和释放资源。
     * </p>
     */
    // @QuartzScheduled(cron = "0 0 3 * * ?")
    @QuartzScheduled(cron = "0 0/5 * * * ?") // TODO 测试时间
    public void execute() {
        runAll();
    }


    public void runAll() {
        LogUtils.info("Start executing all tasks");
        publisher.publishEvent(new ExecuteEvent(this));
        LogUtils.info("All tasks executed successfully");
    }

}
