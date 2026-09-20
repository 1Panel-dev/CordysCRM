package cn.cordys.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    // 核心线程数
    private static final int CORE_POOL_SIZE = 20;
    // 最大线程数
    private static final int MAX_POOL_SIZE = 20;
    // 空闲线程最大存活秒数
    private static final int KEEP_ALIVE_SECONDS = 60;
    // 关闭时最大等待秒数
    private static final int AWAIT_TERMINATION_SECONDS = 60;

    // 统计字段刷新的核心线程数
    private static final int STATISTIC_CORE_POOL_SIZE = 8;
    // 统计字段刷新的最大线程数
    private static final int STATISTIC_MAX_POOL_SIZE = 8;
    // 统计字段刷新的队列容量，必须有界
    private static final int STATISTIC_QUEUE_CAPACITY = 64;

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
     * 统计字段分批刷新的专用线程池。
     *
     * <p>不能复用 {@link #taskExecutor()}: 那个池是无界队列 + CallerRunsPolicy, 而统计刷新任务本身就
     * 跑在该池上(@Async("threadPoolTaskExecutor")), 一旦池内线程都在等自己提交的分页子任务,
     * 子任务会排在无界队列里永远轮不到执行, 整个池就被自己的子任务拖死。专用池把这个自等待闭环拆开,
     * 同时用有界队列 + CallerRunsPolicy 保证满载时提交方自己执行(仍然在推进, 不会死等)。</p>
     */
    @Bean(name = "statisticRefreshExecutor")
    public ThreadPoolTaskExecutor statisticRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(STATISTIC_CORE_POOL_SIZE);
        executor.setMaxPoolSize(STATISTIC_MAX_POOL_SIZE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("cs-async-statistic-");
        executor.setQueueCapacity(STATISTIC_QUEUE_CAPACITY);
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
        return (ex, method, params) -> log.error("异步任务异常: {}", method.getName() + " - " + ex.getMessage());
    }
}