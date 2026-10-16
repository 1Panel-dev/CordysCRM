package cn.cordys.common.service;

import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.crm.system.service.LogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseExportRequestSourceTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void exportRetainsSourceAfterOriginalRequestEnds() throws Exception {
        BaseExportService service = new BaseExportService() {};
        CompletableFuture<LogDTO> captured = new CompletableFuture<>();
        LogService logService = new LogService() {
            @Override
            public void add(LogDTO log) {
                captured.complete(log);
            }
        };
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "exportTaskService", new ExportTaskService() {
            @Override
            public void update(String taskId, String status, String userId) {
            }
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Source", "MCP");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ExportTask task = new ExportTask();
        task.setId("export-1");
        CountDownLatch requestEnded = new CountDownLatch(1);

        service.runExport("org-1", "user-1", "CUSTOMER", Locale.CHINA, task, "customers.xlsx",
                () -> assertTrue(requestEnded.await(5, TimeUnit.SECONDS)));
        RequestContextHolder.resetRequestAttributes();
        requestEnded.countDown();

        LogDTO log = captured.get(5, TimeUnit.SECONDS);
        assertEquals("MCP", log.getRequestSource());
        assertEquals("org-1", log.getOrganizationId());
        assertEquals("user-1", log.getCreateUser());
        assertEquals("export-1", log.getResourceId());
    }
}
