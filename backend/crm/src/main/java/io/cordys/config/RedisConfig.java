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
     * 配置 Redis 频道主题
     * 用于订阅和发布消息
     *
     * @return ChannelTopic 实例
     */
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic(TopicConstants.DOWNLOAD_TOPIC);
    }

    /**
     * 配置Redis消息监听容器
     * 支持并发消息处理和自动重连
     *
     * @param factory         Redis连接工厂
     * @param listenerAdapter 消息监听适配器
     * @return Redis消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic topic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listenerAdapter, topic);
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
