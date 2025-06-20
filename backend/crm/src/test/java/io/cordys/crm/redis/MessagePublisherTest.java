package io.cordys.crm.redis;

import io.cordys.common.redis.MessagePublisher;
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


    @Test
    @Order(0)
    public void publishMessageTest() {
        String message = "Hello, Redis!";
        publisher.publish(message);
        // 这里可以添加断言来验证消息是否成功发布
        // 例如，检查订阅者是否接收到了该消息
    }
}
