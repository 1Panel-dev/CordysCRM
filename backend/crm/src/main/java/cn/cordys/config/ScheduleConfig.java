package cn.cordys.config;

import cn.cordys.common.schedule.ScheduleManager;
import cn.cordys.common.schedule.ScheduleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置类。
 * 配置 Quartz 调度器和定时任务管理服务，启用条件为配置文件中 quartz.enabled 属性为 true。
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    /**
     * 配置 ScheduleManager Bean。
     * 只有在 quartz.enabled 配置为 true 时，ScheduleManager 才会被加载到容器中。
     *
     * @return ScheduleManager 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public ScheduleManager scheduleManager() {
        return new ScheduleManager();
    }

    /**
     * 配置 ScheduleService Bean。
     * 只有在 quartz.enabled 配置为 true 时，ScheduleService 才会被加载到容器中。
     *
     * @return ScheduleService 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public ScheduleService scheduleService() {
        return new ScheduleService();
    }
}
