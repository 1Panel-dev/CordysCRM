package cn.cordys.config;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.security.SessionUser;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 生成随机会话令牌密钥并保存在 Redis 中，供重启和多节点共享。
 */
@Component
public class SessionTokenKeyInitializer {

    private static final String REDIS_KEY = "cordys:security:session-token-key";

    private final StringRedisTemplate redis;

    public SessionTokenKeyInitializer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostConstruct
    public void initialize() {
        String secret = redis.opsForValue().get(REDIS_KEY);
        if (secret == null) {
            // 并发启动时只保留首个密钥，所有节点都读取最终保存的值。
            redis.opsForValue().setIfAbsent(REDIS_KEY, CodingUtils.generateSecretKey());
            secret = redis.opsForValue().get(REDIS_KEY);
        }
        if (secret == null || !secret.matches("[0-9a-f]{32}")) {
            throw new IllegalStateException("Invalid session token key in Redis");
        }
        SessionUser.secret = secret;
    }
}
