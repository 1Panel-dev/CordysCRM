package cn.cordys.common.uid;

import cn.cordys.common.exception.GenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;

@Component
@Slf4j
public class SerialNumGenerator {

    private static final int RULE_SIZE = 5;
    private static final int DATE_KEY_IDX = 3;
    private static final String PREFIX = "serial";

    private final StringRedisTemplate redis;

    public SerialNumGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 按规则生成流水号
     */
    public String generateByRules(List<String> rules, String orgId, String formKey) {
        return generateByRules(rules, orgId, formKey, new Date());
    }

    String generateByRules(List<String> rules, String orgId, String formKey, Date now) {
        if (CollectionUtils.size(rules) < RULE_SIZE) {
            throw new GenericException("流水号规则配置有误");
        }

        Rule r = Rule.from(rules);

        // 展示格式与计数周期分开，同一周期内修改分隔符不会重新取号。
        String date = new SimpleDateFormat(r.datePattern()).format(now);
        String counterDate = new SimpleDateFormat(counterDatePattern(r.datePattern()), Locale.ROOT).format(now);
        String key = "%s:%s:%s:%s:%s".formatted(PREFIX, orgId, formKey, counterDate, r.p1);
        try {
            // Redis 自增序列
            long seq = Objects.requireNonNull(redis.opsForValue().increment(key), "Redis increment 返回 null");
            // 构造最终流水号
            return ("%s%s%s%s%0" + r.width() + "d")
                    .formatted(r.p1(), r.p2(), date, r.mid(), seq);
        } catch (Exception e) {
            log.error("生成流水号失败", e);
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
                    Integer.parseInt(rules.get(4)));
        }
    }

    /**
     * 校验仅包含年、月、日的展示格式，返回对应计数周期的规范格式。
     * 日期必须包含完整年份，以及所选周期需要的月份和日期，防止不同周期输出相同编号。
     *
     * @param datePattern 表单配置的日期格式
     * @return 规范计数日期格式
     * @throws IllegalArgumentException 日期格式不合法或日期信息不完整
     */
    public static String counterDatePattern(String datePattern) {
        if (datePattern == null || datePattern.isBlank()) {
            throw new IllegalArgumentException("流水号日期格式不能为空");
        }
        // 使用现有格式化器校验语法，再忽略引号内的固定字符判断计数粒度。
        new SimpleDateFormat(datePattern);
        String fields = datePattern.replaceAll("'(?:[^']|'')*'", "");
        boolean month = fields.indexOf('M') >= 0;
        boolean day = fields.indexOf('d') >= 0;
        if (!fields.contains("yyyy")
                || fields.chars().anyMatch(ch -> ch < 128 && Character.isLetter(ch) && "yMd".indexOf(ch) < 0)
                || (month && !fields.contains("MM")) || (day && (!month || !fields.contains("dd")))) {
            throw new IllegalArgumentException("流水号日期格式仅支持完整的 yyyy、yyyyMM 或 yyyyMMdd，可添加分隔符");
        }
        return day ? "yyyyMMdd" : month ? "yyyyMM" : "yyyy";
    }

    public void clean() {
        clean(new Date());
    }

    /** 根据业务层提供的组织、表单归属判定，同时清理已无归属的计数。 */
    public void clean(BiPredicate<String, String> formExists) {
        clean(new Date(), formExists);
    }

    /** 规则变更提交后，只清理指定表单不再使用的周期和前缀，保留新规则已有的计数。 */
    public void cleanUnusedRules(String orgId, String formKey, List<String> rules, boolean formulaPrefix) {
        String scope = "%s:%s:%s:".formatted(PREFIX, orgId, formKey);
        int dateLength = rules == null ? 0 : counterDatePattern(rules.get(2)).length();
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(scope + "*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                if (!key.startsWith(scope)) {
                    return;
                }
                // 前缀可以为空或包含冒号，不能再次按冒号拆分前缀。
                String[] parts = key.substring(scope.length()).split(":", 2);
                if (rules == null || parts.length != 2 || !parts[0].matches("\\d{" + dateLength + "}")
                        || (!formulaPrefix && !Objects.equals(parts[1], rules.getFirst()))) {
                    redis.delete(key);
                }
            });
        } catch (Exception e) {
            log.error("清理表单历史流水号Key失败: {}:{}", orgId, formKey, e);
        }
    }

    void clean(Date now) {
        clean(now, (orgId, formKey) -> true);
    }

    void clean(Date now, BiPredicate<String, String> formExists) {
        log.info("开始清理无效及过期流水号Key");
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match("serial:*").count(1000).build())) {
            cursor.forEachRemaining(key -> {
                // 保留空前缀及前缀中的冒号，只解析固定的组织、表单和周期日期。
                String[] ks = key.split(":", RULE_SIZE);
                if (ks.length != RULE_SIZE || ks[1].isBlank() || ks[2].isBlank()
                        || !ks[DATE_KEY_IDX].matches("\\d{4}(\\d{2}){0,2}")) {
                    redis.delete(key);
                    log.info("删除格式不规范的流水号Key: {}", key);
                    return;
                }
                String counterDate = ks[DATE_KEY_IDX];
                String pattern = switch (counterDate.length()) {
                case 4 -> "yyyy";
                case 6 -> "yyyyMM";
                default -> "yyyyMMdd";
                };
                SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.ROOT);
                formatter.setLenient(false);
                try {
                    formatter.parse(counterDate);
                } catch (ParseException e) {
                    redis.delete(key);
                    log.info("删除日期不合法的流水号Key: {}", key);
                    return;
                }
                // 定时清理按周期和表单归属判断；规则变更的历史计数由保存入口单独清理。
                if (counterDate.compareTo(formatter.format(now)) < 0 || !formExists.test(ks[1], ks[2])) {
                    redis.delete(key);
                    log.info("删除过期或已无表单归属的流水号Key: {}", key);
                }
            });
        } catch (Exception e) {
            log.error("流水号Key清理异常: ", e);
        }

        log.info("流水号Key清理完成");
    }
}
