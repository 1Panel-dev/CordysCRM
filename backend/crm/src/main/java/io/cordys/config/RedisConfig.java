package io.cordys.config;

import io.cordys.common.constants.TopicConstants;
import io.cordys.common.redis.MessageSubscriber;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 配置 RedisCacheManager，提供全局的缓存配置。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存有效时间
                .entryTtl(Duration.ofHours(1))
                // 设置 key 和 value 的序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 禁止缓存 null 值
                .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * 配置 RedisTemplate，提供更灵活的 Redis 操作方式。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用 String 序列化器序列化 key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用 JSON 序列化器序列化 value
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置所有 Redis 频道主题集合
     * 用于统一管理订阅和发布的主题
     *
     * @return List<ChannelTopic> Redis主题集合
     */
    @Bean
    public List<ChannelTopic> channelTopics() {
        List<ChannelTopic> topics = new ArrayList<>();
        // 添加所有需要的主题到集合中
        TopicConstants.ALL_TOPICS.forEach(topicName -> topics.add(new ChannelTopic(topicName)));
        return topics;
    }

    /**
     * 配置Redis消息监听容器
     * 支持并发消息处理和自动重连
     *
     * @param factory         Redis连接工厂
     * @param listenerAdapter 消息监听适配器
     * @param channelTopics   Redis主题集合
     * @return Redis消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter listenerAdapter,
            List<ChannelTopic> channelTopics) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 动态添加所有主题的监听器
        for (ChannelTopic topic : channelTopics) {
            container.addMessageListener(listenerAdapter, topic);
        }

        return container;
    }

    /**
     * 配置消息监听适配器
     *
     * @param subscriber 消息订阅处理器
     * @return 消息监听适配器
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(MessageSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        // 使用JSON序列化器处理消息
        adapter.setSerializer(new GenericJackson2JsonRedisSerializer());
        return adapter;
    }

}
