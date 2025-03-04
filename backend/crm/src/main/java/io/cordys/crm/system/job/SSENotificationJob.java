package io.cordys.crm.system.job;

import io.cordys.crm.system.notice.sse.SseService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SSENotificationJob {

    @Resource
    private SseService sseService;

    /**
     * 集群中每个节点都触发，Session 建立在某节点，就由某个节点推送消息
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastPeriodically() {
        sseService.broadcastPeriodically();
    }
}
