package io.cordys.common.interceptor;

import io.cordys.common.util.CompressUtils;
import io.cordys.config.MybatisInterceptorConfig;
import io.cordys.crm.system.domain.MessageTaskBlob;
import io.cordys.crm.system.domain.OperationLogBlob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统拦截器配置类
 * <p>
 * 该类用于配置 MyBatis 的拦截器，特别是字段压缩等功能的配置。
 * </p>
 */
@Configuration
public class SystemInterceptor {

    /**
     * 配置系统拦截器列表
     *
     * @return 返回 MyBatis 拦截器配置列表，目前支持字段压缩等功能。
     */
    @Bean
    public List<MybatisInterceptorConfig> systemCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        // TODO：实现 blob 字段压缩功能
        configList.add(new MybatisInterceptorConfig(MessageTaskBlob.class, "template", CompressUtils.class, "zip", "unzip"));

        configList.add(new MybatisInterceptorConfig(OperationLogBlob.class, "originalValue", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(OperationLogBlob.class, "modifiedValue", CompressUtils.class, "zip", "unzip"));

        // 添加自定义拦截器配置，例如压缩和解压缩功能
        // configList.add(new MybatisInterceptorConfig(TestResourcePoolBlob.class, "configuration", CompressUtils.class, "zip", "unzip"));
        return configList;
    }
}
