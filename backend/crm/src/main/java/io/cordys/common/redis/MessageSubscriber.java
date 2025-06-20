package io.cordys.common.redis;

import io.cordys.common.util.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

/**
 * Redis消息订阅处理器
 * 负责接收并处理来自Redis频道的消息
 */
@Service
public class MessageSubscriber implements MessageListener {

    /**
     * 处理从Redis接收的消息
     *
     * @param message Redis消息对象
     * @param pattern 订阅的模式
     */
    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        try {
            String channel = new String(pattern);
            String messageBody = new String(message.getBody());

            LogUtils.info("接收到来自频道 {} 的消息: {}", channel, messageBody);

            // 处理消息
            processMessage(messageBody, channel);
        } catch (Exception e) {
            LogUtils.error("处理订阅消息时发生异常", e);
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
        LogUtils.info("开始处理消息，频道: {}", channel);

        // 在这里实现具体的业务逻辑
        // TODO: 添加实际的业务处理代码
    }
}