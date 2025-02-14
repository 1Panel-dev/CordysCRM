package io.cordys.listener;

import io.cordys.common.uid.impl.DefaultUidGenerator;
import io.cordys.common.util.HikariCPUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.rsa.RsaKey;
import io.cordys.common.util.rsa.RsaUtils;
import io.cordys.common.util.OnceInterface;
import io.cordys.crm.system.domain.Parameter;
import io.cordys.crm.system.service.ExtScheduleService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.ModuleService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AppListener implements ApplicationRunner {
    @Resource
    private DefaultUidGenerator uidGenerator;

    @Resource
    private ExtScheduleService extScheduleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ModuleService moduleService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;

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

        LogUtils.info("初始化表单字段");
        initOneTime();

        LogUtils.info("===== 完成初始化配置 =====");
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

    private void initOneTime() {
        initOneTime(moduleService::initDefaultOrgModule, "init.module");
        initOneTime(moduleFormService::initForm, "init.form");
    }

    private void initOneTime(OnceInterface onceFunc, final String key) {
        try {
            LambdaQueryWrapper<Parameter> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Parameter::getParamKey, key);
            List<Parameter> parameters = parameterMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isEmpty(parameters)) {
                onceFunc.execute();
                insertParameterOnceKey(key);
            }
        } catch (Throwable e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    private void insertParameterOnceKey(String key) {
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue("done");
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }
}