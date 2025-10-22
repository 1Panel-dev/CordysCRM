package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.service.SystemService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class SessionJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIndexedSessionRepository redisIndexedSessionRepository;

    @Resource
    private SystemService systemService;

    /**
     * 定时清理没有绑定用户的会话。
     * <p>
     * 该方法每晚 0 点 2 分执行，扫描 Redis 中的会话数据，删除没有绑定用户信息的会话。
     * 此外，还会处理一些特殊情况，如 Redisson 设置了过期时间为 -1 时，手动设置过期时间。
     * </p>
     *
     * <p>
     * 该方法使用 {@link QuartzScheduled} 注解定时执行，并使用 {@link ScanOptions} 扫描 Redis 中的会话。
     * </p>
     * <p>
     * spring.session.timeout=30d
     * server.servlet.session.timeout=30d
     */
    @QuartzScheduled(cron = "0 2 0 * * ?")
    public void cleanSession() {
        Map<String, Long> userCount = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match("spring:session:sessions:*").count(1000).build();

        try (Cursor<String> scan = stringRedisTemplate.scan(options)) {
            while (scan.hasNext()) {
                String key = scan.next();
                if (key.contains("spring:session:sessions:expires:")) {
                    continue;
                }

                String sessionId = key.substring(key.lastIndexOf(":") + 1);
                Boolean exists = stringRedisTemplate.opsForHash().hasKey(key, "sessionAttr:user");

                // 删除没有绑定用户的会话
                if (!exists) {
                    redisIndexedSessionRepository.deleteById(sessionId);
                } else {
                    // 获取用户信息并检查会话过期时间
                    Object user = redisIndexedSessionRepository.getSessionRedisOperations().opsForHash().get(key, "sessionAttr:user");
                    Long expire = redisIndexedSessionRepository.getSessionRedisOperations().getExpire(key);

                    assert user != null;
                    String userId = (String) MethodUtils.invokeMethod(user, "getId");
                    userCount.merge(userId, 1L, Long::sum);

                    // 记录日志并检查会话的过期时间
                    LogUtils.info("{} : {} 过期时间: {}", key, userId, expire);

                    // 如果过期时间为 -1，则手动设置过期时间为 30 秒
                    if (expire != null && expire == -1) {
                        redisIndexedSessionRepository.getSessionRedisOperations().expire(key, Duration.of(30, ChronoUnit.SECONDS));
                    }
                }
            }
            // 清理缓存
            systemService.clearFormCache();
            LogUtils.info("用户会话统计: {}", JSON.toJSONString(userCount));
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }
}
