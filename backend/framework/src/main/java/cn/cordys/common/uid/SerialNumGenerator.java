package cn.cordys.common.uid;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.cordys.quartz.anno.QuartzScheduled;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class SerialNumGenerator {

    private static final int RULE_SIZE = 5;
    private static final String PREFIX = "serial";

    private final StringRedisTemplate redis;

    public SerialNumGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 按规则生成流水号
     */
    public String generateByRules(List<String> rules, String orgId, String formKey) {
        if (CollectionUtils.size(rules) < RULE_SIZE) {
            throw new GenericException("流水号规则配置有误");
        }

        Rule r = Rule.from(rules);

        // 强制使用年月作为流水号 key 的日期部分
        String date = new SimpleDateFormat(r.datePattern()).format(new Date());
        String key = "%s:%s:%s:%s".formatted(PREFIX, orgId, formKey, date);
        try {
            // Redis 自增序列
            long seq = Objects.requireNonNull(redis.opsForValue().increment(key), "Redis increment 返回 null");
            // 构造最终流水号
            return ("%s%s%s%s%0" + r.width() + "d")
                    .formatted(r.p1(), r.p2(), date, r.mid(), seq);

        } catch (Exception e) {
            LogUtils.error("生成流水号失败", e);
            return null;
        }
    }

    /**
     * 内部规则封装
     */
    private record Rule(String p1, String p2, String datePattern, String mid, int width) {
        static Rule from(List<String> rules) {
            return new Rule(
                    rules.get(0),
                    rules.get(1),
                    rules.get(2),
                    rules.get(3),
                    Integer.parseInt(rules.get(4))
            );
        }
    }

    @QuartzScheduled(cron = "0 0 1 1,16 * ?")
    public void clean() {
        LogUtils.info("开始清理过期流水号");

        String currentMonth = new SimpleDateFormat("yyyyMM").format(new Date());

        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match("serial:*:*:*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                String serialDate = key.substring(key.lastIndexOf(":") + 1);
                if (!currentMonth.equals(serialDate)) {
                    redis.delete(key);
                    LogUtils.info("删除过期Key: {}", key);
                }
            });
        } catch (Exception e) {
            LogUtils.error("流水号过期Key清理异常: ", e);
        }

        LogUtils.info("流水号过期Key清理完成");
    }
}
