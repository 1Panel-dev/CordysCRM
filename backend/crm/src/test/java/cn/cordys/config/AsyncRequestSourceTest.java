package cn.cordys.config;

import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.aspectj.handler.OperationLogService;
import cn.cordys.common.util.ServletUtils;
import cn.cordys.context.OrganizationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncRequestSourceTest {

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        OrganizationContext.clear();
        executor = new AsyncConfig().taskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        try {
            executor.shutdown();
        } finally {
            RequestContextHolder.resetRequestAttributes();
            OrganizationContext.clear();
        }
    }

    @Test
    void asyncLogsKeepTheSourceAfterRequestEndsWithoutLeakingToTheNextTask() throws Exception {
        bindRequest("SKILL");
        AtomicReference<Thread> worker = new AtomicReference<>();
        FutureTask<String> first = submit(() -> {
            worker.set(Thread.currentThread());
            assertNull(ServletUtils.getRequest());
            return new LogDTO().getRequestSource();
        });
        RequestContextHolder.resetRequestAttributes();

        assertEquals("SKILL", first.get(5, TimeUnit.SECONDS));
        FutureTask<String> next = submit(() -> {
            assertSame(worker.get(), Thread.currentThread());
            assertNull(OrganizationContext.getRequestSource());
            return new LogDTO().getRequestSource();
        });
        assertNull(next.get(5, TimeUnit.SECONDS));
    }

    @Test
    void httpSourceOverridesTaskContextAndContextPropagatesWithoutARequest() throws Exception {
        OrganizationContext.setRequestSource("MCP");
        bindRequest("SKILL");

        assertEquals("SKILL", ServletUtils.getRequestSource());
        assertEquals("SKILL", submit(() -> new LogDTO().getRequestSource()).get(5, TimeUnit.SECONDS));
        assertEquals("MCP", OrganizationContext.getRequestSource());

        RequestContextHolder.resetRequestAttributes();
        assertEquals("MCP", ServletUtils.getRequestSource());
        assertEquals("MCP", submit(() -> new LogDTO().getRequestSource()).get(5, TimeUnit.SECONDS));
    }

    @Test
    void unqualifiedAsyncUsesTheDecoratedApplicationExecutor() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(AsyncConfig.class, AsyncProbe.class)) {
            bindRequest("MCP");

            CapturedSource captured = context.getBean(AsyncProbe.class).capture().get(5, TimeUnit.SECONDS);

            assertEquals("MCP", captured.source());
            assertTrue(captured.threadName().startsWith("cs-async-task-"));
        }
    }

    private void bindRequest(String source) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OperationLogService.REQUEST_SOURCE_HEADER, source);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private <T> FutureTask<T> submit(Callable<T> action) {
        FutureTask<T> task = new FutureTask<>(action);
        executor.execute(task);
        return task;
    }

    public static class AsyncProbe {
        @Async
        public CompletableFuture<CapturedSource> capture() {
            return CompletableFuture.completedFuture(new CapturedSource(
                    new LogDTO().getRequestSource(), Thread.currentThread().getName()));
        }
    }

    private record CapturedSource(String source, String threadName) {
    }
}
