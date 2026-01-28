package cn.cordys.crm.system.consumer;

import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.redis.TopicConsumer;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import cn.cordys.crm.system.notice.sse.SseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SSEConsumer implements TopicConsumer {

  @Resource private SseService sseService;

  @Override
  public String getChannel() {
    return TopicConstants.SSE_TOPIC;
  }

  @Override
  public void consume(String message) {
    NoticeRedisMessage noticeRedisMessage = JSON.parseObject(message, NoticeRedisMessage.class);
    sseService.broadcastPeriodically(
        noticeRedisMessage.getMessage(), noticeRedisMessage.getNoticeType());
  }
}
