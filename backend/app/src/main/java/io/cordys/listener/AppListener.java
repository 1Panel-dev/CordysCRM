package io.cordys.listener;

import io.cordys.common.service.DataInitService;
import io.cordys.common.uid.impl.DefaultUidGenerator;
import io.cordys.common.util.HikariCPUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.rsa.RsaKey;
import io.cordys.common.util.rsa.RsaUtils;
import io.cordys.crm.system.service.ExportTaskStopService;
import io.cordys.crm.system.service.ExtScheduleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class AppListener implements ApplicationRunner {
    @Resource
    private DefaultUidGenerator uidGenerator;

    @Resource
    private ExtScheduleService extScheduleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DataInitService dataInitService;

    @Resource
    private ExportTaskStopService exportTaskStopService;
    @Resource
    private CacheManager cacheManager;

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
        LogUtils.info("===== 开始初始化配置 =====");

        // 初始化唯一ID生成器
        uidGenerator.init();

        // 初始化RSA配置
        LogUtils.info("初始化RSA配置");
        initializeRsaConfiguration();

        LogUtils.info("初始化定时任务");
        extScheduleService.startEnableSchedules();

        HikariCPUtils.printHikariCPStatus();

        LogUtils.info("初始化默认组织数据");
        dataInitService.initOneTime();

        LogUtils.info("停止导出任务");
        exportTaskStopService.stopPreparedAll();

        LogUtils.info("清理表单缓存");
        clearFormCache();

        LogUtils.info("===== 完成初始化配置 =====");
    }

    /**
     * 清理表单相关缓存
     * <p>
     * 清理表单缓存和表结构缓存，确保应用使用最新的表单配置和结构
     * </p>
     */
    public void clearFormCache() {
        final String[] cacheNames = {"form_cache", "table_schema_cache"};

        for (String cacheName : cacheNames) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    LogUtils.info("成功清理缓存：{}", cacheName);
                }
            } catch (Exception ignored) {
            }
        }
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
            LogUtils.error("从 Redis 获取 RSA 配置失败", e);
        }

        try {
            // 如果 Redis 中没有密钥，生成新的 RSA 密钥并保存到 Redis
            RsaKey rsaKey = RsaUtils.getRsaKey();
            stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(rsaKey));
            RsaUtils.setRsaKey(rsaKey);
        } catch (Exception e) {
            LogUtils.error("初始化 RSA 配置失败", e);
        }
    }


}