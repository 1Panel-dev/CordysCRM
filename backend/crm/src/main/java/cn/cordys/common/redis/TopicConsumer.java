package cn.cordys.common.redis;

/**
 * redis 各种topic消费者的公共接口
 */
public interface TopicConsumer {

    String getChannel();

    void consume(String message);
}
