package cn.cordys.common.redis;

import cn.cordys.common.constants.TopicConstants;
import cn.cordys.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.SubscriptionListener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MessageSubscriberTest {

    @Test
    @Timeout(10)
    void configuredContainerDispatchesRawMessagesWithoutAnAdapter() throws Exception {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        AtomicReference<MessageListener> listener = new AtomicReference<>();
        doAnswer(invocation -> {
            MessageListener subscribed = invocation.getArgument(0);
            byte[][] channels = (byte[][]) invocation.getRawArguments()[1];
            assertThat(Arrays.stream(channels).map(channel -> new String(channel, StandardCharsets.UTF_8)))
                    .containsExactlyInAnyOrderElementsOf(TopicConstants.ALL_TOPICS);
            listener.set(subscribed);
            long count = 0;
            for (byte[] channel : channels) {
                ((SubscriptionListener) subscribed).onChannelSubscribed(channel, ++count);
            }
            return null;
        }).when(connection).subscribe(any(MessageListener.class), any(byte[][].class));

        TopicConsumer sse = consumer(TopicConstants.SSE_TOPIC);
        TopicConsumer download = consumer(TopicConstants.DOWNLOAD_TOPIC);
        MessageSubscriber subscriber = new MessageSubscriber(List.of(sse, download));
        RedisConfig config = new RedisConfig();
        var container = config.redisContainer(factory, subscriber, config.channelTopics());
        container.setTaskExecutor(Runnable::run);
        container.setSubscriptionExecutor(Runnable::run);
        container.afterPropertiesSet();
        try {
            container.start();
            String notice = "{\"message\":\"用户一\",\"noticeType\":\"SYSTEM_NOTICE\"}";
            listener.get().onMessage(message(TopicConstants.SSE_TOPIC, notice), null);
            listener.get().onMessage(message(TopicConstants.DOWNLOAD_TOPIC, "task-1"), null);

            verify(sse).consume(notice);
            verify(download).consume("task-1");
        } finally {
            container.destroy();
        }
    }

    @Test
    void routesByMessageChannelEvenWhenPatternIsAbsentOrDifferent() {
        TopicConsumer consumer = consumer(TopicConstants.SSE_TOPIC);
        MessageSubscriber subscriber = new MessageSubscriber(List.of(consumer));

        subscriber.onMessage(message(TopicConstants.SSE_TOPIC, "中文通知"), null);
        subscriber.onMessage(message(TopicConstants.SSE_TOPIC, "模式订阅"), "*-topic".getBytes(StandardCharsets.UTF_8));

        verify(consumer).getChannel();
        verify(consumer).consume("中文通知");
        verify(consumer).consume("模式订阅");
        verifyNoMoreInteractions(consumer);
    }

    private TopicConsumer consumer(String channel) {
        TopicConsumer consumer = mock(TopicConsumer.class);
        when(consumer.getChannel()).thenReturn(channel);
        return consumer;
    }

    private DefaultMessage message(String channel, String body) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }
}
