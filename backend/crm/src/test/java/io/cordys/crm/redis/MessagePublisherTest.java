package io.cordys.crm.redis;

import io.cordys.common.constants.TopicConstants;
import io.cordys.common.redis.MessagePublisher;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.consumer.SSEConsumer;
import io.cordys.crm.system.notice.dto.NoticeRedisMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessagePublisherTest {


    @Resource
    private MessagePublisher publisher;

    @Resource
    private SSEConsumer sseConsumer;

    @Test
    @Order(0)
    public void publishMessageTest() throws InterruptedException {


        NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
        noticeRedisMessage.setMessage("admin");
        noticeRedisMessage.setNoticeType(NotificationConstants.Type.SYSTEM_NOTICE.toString());
        publisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
        // 这里可以添加断言来验证消息是否成功发布
// 等待异步消息处理
        Thread.sleep(1000); // 可根据实际延迟调整
        System.out.println(sseConsumer.getChannel());


    }



}
