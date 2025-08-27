package cn.cordys.config;

import cn.cordys.common.util.LogUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // 核心线程数
    private static final int CORE_POOL_SIZE = 20;
    // 最大线程数
    private static final int MAX_POOL_SIZE = 20;
    // 空闲线程最大存活秒数
    private static final int KEEP_ALIVE_SECONDS = 60;
    // 关闭时最大等待秒数
    private static final int AWAIT_TERMINATION_SECONDS = 60;

    // 同时暴露默认名称，便于 @Async 自动装配
    @Bean(name = {"threadPoolTaskExecutor", "applicationTaskExecutor"})
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("cs-async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        return executor;
    }

    /**
     * 捕获 @Async void 方法未处理的异常
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> LogUtils.error("异步任务异常: " + method.getName() + " - " + ex.getMessage());
    }
}