package cn.cordys.crm.system.service;

import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.aspectj.handler.OperationLogHandler;
import cn.cordys.aspectj.handler.OperationLogService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.ServletUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.OperationLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogRequestSourceTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
        OrganizationContext.clear();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "SKILL,key,secret,SKILL",
            "API,key,secret,API",
            "MCP,key,secret,MCP",
            "SKILL,NULL,NULL,SKILL",
            "NULL,key,secret,API",
            "'  ',key,secret,API",
            "NULL,key,NULL,WEB",
            "NULL,NULL,secret,WEB",
            "NULL,NULL,NULL,WEB"
    }, nullValues = "NULL")
    void manualAndAnnotatedLogsUseTheSameRequestSource(String source, String accessKey, String secretKey, String expected) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/customer/batch-transfer");
        if (source != null) {
            request.addHeader(OperationLogService.REQUEST_SOURCE_HEADER, source);
        }
        if (accessKey != null) {
            request.addHeader(OperationLogService.ACCESS_KEY_HEADER, accessKey);
        }
        if (secretKey != null) {
            request.addHeader(OperationLogService.SECRET_KEY_HEADER, secretKey);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals(expected, ServletUtils.getRequestSource());
        assertEquals(expected, new LogDTO().getRequestSource());
        assertEquals(expected, manualLog().getRequestSource());

        AtomicReference<LogDTO> captured = new AtomicReference<>();
        OperationLogService service = new OperationLogService();
        ReflectionTestUtils.setField(service, "operationLogHandler", (OperationLogHandler) captured::set);
        service.record(new cn.cordys.aspectj.builder.OperationLog());

        assertEquals(expected, captured.get().getRequestSource());
        assertEquals("POST", captured.get().getMethod());
        assertEquals("/customer/batch-transfer", captured.get().getPath());
    }

    @Test
    void backgroundLogsDoNotInventAWebSource() {
        RequestContextHolder.resetRequestAttributes();
        OrganizationContext.clear();

        assertNull(ServletUtils.getRequestSource());
        assertNull(new LogDTO().getRequestSource());
        assertNull(manualLog().getRequestSource());
    }

    @Test
    void capturedSourceSurvivesRequestCleanupAndMappingOnAnotherThread() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OperationLogService.REQUEST_SOURCE_HEADER, "SKILL");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        LogDTO defaultLog = new LogDTO();
        LogDTO manualLog = manualLog();
        RequestContextHolder.resetRequestAttributes();

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                assertNull(ServletUtils.getRequestSource());
                assertEquals("SKILL", defaultLog.getRequestSource());
                assertEquals("SKILL", manualLog.getRequestSource());
                assertEquals("SKILL", BeanUtils.copyBean(new OperationLog(), defaultLog).getRequestSource());
                assertEquals("SKILL", BeanUtils.copyBean(new OperationLog(), manualLog).getRequestSource());
            }).get(5, TimeUnit.SECONDS);
        }
    }

    private LogDTO manualLog() {
        return new LogDTO("test-org", "customer-1", "operator", "UPDATE", "CUSTOMER", "Customer");
    }
}
