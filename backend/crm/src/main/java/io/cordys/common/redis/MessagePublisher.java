package io.cordys.common.redis;

import io.cordys.common.constants.TopicConstants;
import io.cordys.common.util.LogUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisher {

    private final StringRedisTemplate redisTemplate;

    public MessagePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发布消息到默认主题
     *
     * @param message 要发布的消息内容
     */
    public void publish(String message) {
        publish(TopicConstants.DOWNLOAD_TOPIC, message);
    }

    /**
     * 发布消息到指定主题
     *
     * @param topicName 主题名称
     * @param message   要发布的消息内容
     */
    public void publish(String topicName, String message) {
        try {
            ChannelTopic topic = new ChannelTopic(topicName);
            redisTemplate.convertAndSend(topic.getTopic(), message);
            LogUtils.info("消息已发布到主题 {}: {}", topicName, message);
        } catch (Exception e) {
            LogUtils.error("发布消息到主题失败", e);
        }
    }
}