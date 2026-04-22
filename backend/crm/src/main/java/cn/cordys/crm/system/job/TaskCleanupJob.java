package cn.cordys.crm.system.job;

import cn.cordys.crm.system.job.listener.ExecuteEvent;
import cn.cordys.quartz.anno.QuartzScheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 系统定时清理任务调度器
 * <p>
 * 每天凌晨 3 点触发，通过 ApplicationEvent 分发到各个 Listener 执行清理。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskCleanupJob {

    private final ApplicationEventPublisher publisher;

    @QuartzScheduled(cron = "0 0 3 * * ?")
    public void execute() {
        runAll();
    }

    public void runAll() {
        log.info("开始执行所有清理任务");
        publisher.publishEvent(new ExecuteEvent(this));
        log.info("所有清理任务执行完成");
    }
}
