package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.crm.system.notice.sse.SseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SSENotificationJob {

    @Resource
    private SseService sseService;

    /**
     * 每 5 秒向对应客户端推送一次消息
     */
    @QuartzScheduled(fixedRate = 5000)
    public void broadcastPeriodically() {
        sseService.broadcastPeriodically();
    }
}
