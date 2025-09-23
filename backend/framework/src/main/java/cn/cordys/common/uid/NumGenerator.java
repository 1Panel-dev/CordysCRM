package cn.cordys.common.uid;

import cn.cordys.common.constants.ApplicationNumScope;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RIdGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用于生成数字 ID 的生成器类，支持根据不同的应用场景（Scope）生成唯一的 ID。
 *
 * <p>该类使用 Redisson 的分布式 ID 生成器和 Redis 进行管理。</p>
 */
@Component
public class NumGenerator {

    // 初始值，代表从100001开始生成 ID
    private static final long INIT = 100001L;

    // 限制每次生成的最大数量
    private static final long LIMIT = 1;

    // Redisson 实例，用于获取分布式 ID 生成器
    private static Redisson redisson;

    // StringRedisTemplate 用于操作 Redis
    private static StringRedisTemplate stringRedisTemplate;

    // 特定的子范围，用于表示二级的用例
    private static final List<ApplicationNumScope> SUB_NUM = List.of(ApplicationNumScope.SYSTEM);

    /**
     * 根据指定的应用场景生成唯一的数字 ID。
     *
     * @param scope 应用场景（例如：接口用例）
     *
     * @return 唯一的数字 ID
     */
    public static long nextNum(ApplicationNumScope scope) {
        return nextNum(scope.name(), scope);
    }

    /**
     * 根据指定的前缀和应用场景生成唯一的数字 ID。
     *
     * @param prefix 前缀，例如：ORGANIZATION_ID + "_" + DOMAIN
     * @param scope  应用场景（例如：接口用例）
     *
     * @return 唯一的数字 ID
     */
    public static long nextNum(String prefix, ApplicationNumScope scope) {
        // 获取分布式 ID 生成器
        RIdGenerator idGenerator = redisson.getIdGenerator(prefix + "_" + scope.name());

        // 处理子范围的用例（如 SYSTEM）
        if (SUB_NUM.contains(scope)) {
            // 确保 ID 生成器存在，如果不存在则初始化
            if (!idGenerator.isExists()) {
                idGenerator.tryInit(1, LIMIT);
            }
            // 返回格式化后的 ID，保留 3 位
            return Long.parseLong(prefix.split("_")[1] + StringUtils.leftPad(String.valueOf(idGenerator.nextId()), 3, "0"));
        } else {
            // 其他范围的用例，初始化 ID 生成器
            if (!idGenerator.isExists()) {
                idGenerator.tryInit(INIT, LIMIT);
            }
            return idGenerator.nextId();
        }
    }

    /**
     * 设置 Redisson 实例，用于分布式 ID 生成器。
     *
     * @param redisson Redisson 实例
     */
    @Resource
    public void setRedisson(Redisson redisson) {
        NumGenerator.redisson = redisson;
    }

    /**
     * 设置 StringRedisTemplate 实例，用于操作 Redis。
     *
     * @param stringRedisTemplate StringRedisTemplate 实例
     */
    @Resource
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        NumGenerator.stringRedisTemplate = stringRedisTemplate;
    }
}
