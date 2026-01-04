package cn.cordys.crm;

import cn.cordys.common.service.DataInitService;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.util.HikariCPUtils;
import cn.cordys.common.util.JSON;

import cn.cordys.common.util.rsa.RsaKey;
import cn.cordys.common.util.rsa.RsaUtils;
import cn.cordys.crm.system.service.ExtScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class TestAppListener implements ApplicationRunner {
    @Resource
    private DefaultUidGenerator uidGenerator;

    @Resource
    private ExtScheduleService extScheduleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DataInitService dataInitService;

    /**
     * 应用启动后执行的初始化方法。
     * <p>
     * 此方法会依次初始化唯一 ID 生成器、MinIO 配置和 RSA 配置。
     * </p>
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 开始初始化配置 =====");

        // 初始化唯一ID生成器
        uidGenerator.init();

        // 初始化RSA配置
        log.info("初始化RSA配置");
        initializeRsaConfiguration();

        log.info("初始化定时任务");
        extScheduleService.startEnableSchedules();

        log.info("初始化默认组织数据");
        dataInitService.initOneTime();

        HikariCPUtils.printHikariCPStatus();

        log.info("===== 完成初始化配置 =====");
    }

    /**
     * 初始化 RSA 配置。
     * <p>
     * 此方法首先尝试加载现有的 RSA 密钥。如果不存在，则生成新的 RSA 密钥并保存到文件系统。
     * </p>
     */
    private void initializeRsaConfiguration() {
        String redisKey = "rsa:key";
        try {
            // 从 Redis 获取 RSA 密钥
            String rsaStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotBlank(rsaStr)) {
                // 如果 RSA 密钥存在，反序列化并设置密钥
                RsaKey rsaKey = JSON.parseObject(rsaStr, RsaKey.class);
                RsaUtils.setRsaKey(rsaKey);
                return;
            }
        } catch (Exception e) {
            log.error("从 Redis 获取 RSA 配置失败", e);
        }

        try {
            // 如果 Redis 中没有密钥，生成新的 RSA 密钥并保存到 Redis
            RsaKey rsaKey = RsaUtils.getRsaKey();
            stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(rsaKey));
            RsaUtils.setRsaKey(rsaKey);
        } catch (Exception e) {
            log.error("初始化 RSA 配置失败", e);
        }
    }
}