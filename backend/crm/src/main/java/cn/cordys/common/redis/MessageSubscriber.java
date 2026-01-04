package cn.cordys.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis消息订阅处理器
 * 负责接收并处理来自Redis频道的消息
 */
@Service
@Slf4j
public class MessageSubscriber implements MessageListener {

    /**
     * 所有Topic的消费者集合
     */
    static Map<String, TopicConsumer> consumerMap = new HashMap<>();


    public MessageSubscriber(List<TopicConsumer> consumers) {
        consumers.forEach(consumer -> consumerMap.put(consumer.getChannel(), consumer));
    }

    /**
     * 处理从Redis接收的消息
     *
     * @param message Redis消息对象
     * @param pattern 订阅的模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(pattern);
            String messageBody = new String(message.getBody());
            // 处理消息
            processMessage(messageBody, channel);
        } catch (Exception e) {
            log.error("处理订阅消息时发生异常", e);
        }
    }

    /**
     * 处理解析后的消息
     *
     * @param message 消息内容
     * @param channel 消息来源频道
     */
    private void processMessage(String message, String channel) {
        // 根据频道区分不同的业务逻辑处理
        TopicConsumer consumer = consumerMap.get(channel);
        if (consumer == null) {
            log.error("未找到对应的消费者处理频道: {}", channel);
            return;
        }
        consumer.consume(message);
    }


}