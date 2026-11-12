package cn.cordys.common.redis;

import cn.cordys.common.constants.TopicConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MessagePublisherTest {
    @Mock
    private StringRedisTemplate redis;

    private MessagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MessagePublisher(redis);
    }

    @Test
    void publishesToDefaultDownloadTopic() {
        publisher.publish("task-1");

        verify(redis).convertAndSend(TopicConstants.DOWNLOAD_TOPIC, "task-1");
        verifyNoMoreInteractions(redis);
    }

    @Test
    void preservesExplicitTopicAndMessage() {
        String message = "{\"message\":\"admin\",\"noticeType\":\"SYSTEM_NOTICE\"}";

        publisher.publish(TopicConstants.SSE_TOPIC, message);

        verify(redis).convertAndSend(TopicConstants.SSE_TOPIC, message);
        verifyNoMoreInteractions(redis);
    }

    @Test
    void redisFailureDoesNotInterruptCaller() {
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(redis).convertAndSend(TopicConstants.DOWNLOAD_TOPIC, "task-1");

        assertDoesNotThrow(() -> publisher.publish("task-1"));

        verify(redis).convertAndSend(TopicConstants.DOWNLOAD_TOPIC, "task-1");
        verifyNoMoreInteractions(redis);
    }
}
