package io.cordys.crm.system.consumer;

import io.cordys.common.constants.TopicConstants;
import io.cordys.crm.system.notice.dto.NoticeRedisMessage;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.notice.sse.SseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SSEConsumer implements TopicConsumer {

    @Resource
    private SseService sseService;

    @Override
    public String getChannel() {
        return TopicConstants.SSE_TOPIC;
    }

    @Override
    public void consume(String message) {
        NoticeRedisMessage noticeRedisMessage = JSON.parseObject(message, NoticeRedisMessage.class);
        sseService.broadcastPeriodically(noticeRedisMessage.getMessage(), noticeRedisMessage.getNoticeType());
    }
}
