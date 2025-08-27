package cn.cordys.crm.system.job;

import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.job.listener.ExecuteEvent;
import com.fit2cloud.quartz.anno.QuartzScheduled;
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
    @QuartzScheduled(cron = "0 0 3 * * ?")
    public void execute() {
        runAll();
    }


    public void runAll() {
        LogUtils.info("开始执行所有清理任务");
        publisher.publishEvent(new ExecuteEvent(this));
        LogUtils.info("所有清理任务执行完成");
    }

}
