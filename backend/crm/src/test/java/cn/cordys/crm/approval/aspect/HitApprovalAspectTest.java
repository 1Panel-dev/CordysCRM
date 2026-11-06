package cn.cordys.crm.approval.aspect;

import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.dto.response.ApprovalPushResponse;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HitApprovalAspectTest {

    private HitApprovalAspect aspect;
    private ApprovalResourceService resourceService;
    private MockHttpServletRequest request;
    private ApprovalPushParam param;

    @BeforeEach
    void setUp() {
        aspect = new HitApprovalAspect();
        resourceService = mock(ApprovalResourceService.class);
        ReflectionTestUtils.setField(aspect, "approvalResourceService", resourceService);
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        param = ApprovalPushParam.builder().resourceId("resource-1").formKey("custom-form-1")
                .orgId("org-1").userId("user-1").executeTimingEnum(ExecuteTimingEnum.UPDATE).build();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void updateResubmissionReportsFallbackOnlyAfterSuccessfulPush(boolean transactionActive, boolean invalid) {
        ApprovalPushResponse result = new ApprovalPushResponse();
        result.setResubmitNodeInvalid(invalid);
        when(resourceService.pushInNewTransaction(param)).thenReturn(result);
        if (transactionActive) {
            TransactionSynchronizationManager.initSynchronization();
        }

        ReflectionTestUtils.invokeMethod(aspect, "pushAfterCommit", param);

        if (transactionActive) {
            verifyNoInteractions(resourceService);
            assertNull(request.getAttribute(ApprovalPushResponse.class.getName()));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        }
        verify(resourceService).pushInNewTransaction(param);
        assertSame(invalid ? result : null, request.getAttribute(ApprovalPushResponse.class.getName()));
    }

    @Test
    void rolledBackUpdateDoesNotPushOrReportFallback() {
        TransactionSynchronizationManager.initSynchronization();

        ReflectionTestUtils.invokeMethod(aspect, "pushAfterCommit", param);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization
                        .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(resourceService);
        assertNull(request.getAttribute(ApprovalPushResponse.class.getName()));
    }

    @Test
    void updateWithoutHttpRequestStillPushesWhenResubmitNodeIsMissing() {
        RequestContextHolder.resetRequestAttributes();
        ApprovalPushResponse result = new ApprovalPushResponse();
        result.setResubmitNodeInvalid(true);
        when(resourceService.pushInNewTransaction(param)).thenReturn(result);

        ReflectionTestUtils.invokeMethod(aspect, "pushAfterCommit", param);

        verify(resourceService).pushInNewTransaction(param);
    }

    @Test
    void deleteApprovalDoesNotReportUpdateResubmissionFallback() {
        param.setExecuteTimingEnum(ExecuteTimingEnum.DELETE);
        ApprovalPushResponse result = new ApprovalPushResponse();
        result.setResubmitNodeInvalid(true);
        when(resourceService.pushInNewTransaction(param)).thenReturn(result);

        ReflectionTestUtils.invokeMethod(aspect, "pushAfterCommit", param);

        verify(resourceService).pushInNewTransaction(param);
        assertNull(request.getAttribute(ApprovalPushResponse.class.getName()));
    }
}
