package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.service.SystemService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SessionJob {

    private static final String SESSION_KEY_PREFIX = "spring:session:sessions:";
    private static final String SESSION_EXPIRES_PREFIX = SESSION_KEY_PREFIX + "expires:";
    private static final String USER_ATTR_KEY = "sessionAttr:user";
    private static final Duration EXPIRE_FIX_DURATION = Duration.ofSeconds(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIndexedSessionRepository redisIndexedSessionRepository;

    @Resource
    private SystemService systemService;

    /**
     * 定时清理未绑定用户的会话，每晚 0 点 2 分执行。
     */
    @QuartzScheduled(cron = "0 2 0 * * ?")
    public void cleanSession() {
        Map<String, Long> userCount = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(SESSION_KEY_PREFIX + "*")
                .count(1000)
                .build();

        try (Cursor<String> scan = stringRedisTemplate.scan(options)) {
            scan.forEachRemaining(key -> {
                if (key.startsWith(SESSION_EXPIRES_PREFIX)) return;

                String sessionId = key.substring(key.lastIndexOf(":") + 1);
                Boolean hasUser = stringRedisTemplate.opsForHash().hasKey(key, USER_ATTR_KEY);

                if (!hasUser) {
                    redisIndexedSessionRepository.deleteById(sessionId);
                    return;
                }

                Session session = redisIndexedSessionRepository.findById(sessionId);
                if (session == null) return;

                Object principal = session.getAttribute("user");
                if (principal == null) {
                    redisIndexedSessionRepository.deleteById(sessionId);
                    return;
                }

                String userId = extractUserId(principal);
                if (userId == null) {
                    redisIndexedSessionRepository.deleteById(sessionId);
                    return;
                }

                userCount.merge(userId, 1L, Long::sum);

                Long expire = redisIndexedSessionRepository.getSessionRedisOperations().getExpire(key);
                log.info("{} : {} 过期时间: {}", key, userId, expire);

                if (expire != null && expire == -1) {
                    redisIndexedSessionRepository.getSessionRedisOperations()
                            .expire(key, EXPIRE_FIX_DURATION);
                }
            });

            systemService.clearFormCache();
            log.info("用户会话统计: {}", JSON.toJSONString(userCount));
        } catch (Exception e) {
            log.error("清理会话异常", e);
        }
    }

    /**
     * 从 principal 对象中提取 userId。
     * 支持标准 UserDetails 实现（getId()）和 Map 结构（"id" key）。
     */
    private static String extractUserId(Object principal) {
        try {
            if (principal instanceof Map<?, ?> map) {
                Object id = map.get("id");
                return id != null ? id.toString() : null;
            }
            // 通过反射兜底，兼容无源码类型
            return (String) principal.getClass().getMethod("getId").invoke(principal);
        } catch (Exception e) {
            log.warn("无法从 principal 提取 userId: {}", principal.getClass().getName());
            return null;
        }
    }
}
