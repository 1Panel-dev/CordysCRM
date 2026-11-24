package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.constants.FollowUpPlanStatusType;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 跟进计划状态更新的操作日志: 状态变更必须写入日志快照, 状态没变则不应产生日志.
 */
class FollowUpPlanStatusLogTest {
    private static final String USER = "user-a";
    private static final String ORG = "org-a";
    private static final String PLAN_ID = "plan-a";
    private static final String CUSTOMER = "customer-a";
    private static final String CUSTOMER_NAME = "客户A";

    private final FollowUpPlanService service = new FollowUpPlanService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<FollowUpPlan> plans = mock(BaseMapper.class);
    private final FollowUpPlanFieldService fields = mock(FollowUpPlanFieldService.class);
    private final BaseService base = mock(BaseService.class);
    private final FollowUpPlan plan = new FollowUpPlan();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        plan.setId(PLAN_ID);
        plan.setOrganizationId(ORG);
        plan.setCustomerId(CUSTOMER);
        plan.setType("CUSTOMER");
        plan.setOwner(USER);
        plan.setStatus(FollowUpPlanStatusType.PREPARED.name());
        plan.setConverted(false);
        when(plans.selectByPrimaryKey(PLAN_ID)).thenReturn(plan);
        when(base.getCustomerMap(anyList())).thenReturn(Map.of(CUSTOMER, CUSTOMER_NAME));
        ReflectionTestUtils.setField(service, "followUpPlanMapper", plans);
        ReflectionTestUtils.setField(service, "followUpPlanFieldService", fields);
        ReflectionTestUtils.setField(service, "baseService", base);
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void tearDown() {
        OperationLogContext.clear();
        translator.close();
    }

    @Test
    void updateStatusDeclaresOperationLogAnnotation() throws NoSuchMethodException {
        // 没有注解, 日志切面根本不会拦截这个方法
        OperationLog annotation = FollowUpPlanService.class
                .getMethod("updateStatus", FollowUpPlanStatusRequest.class, String.class, String.class)
                .getAnnotation(OperationLog.class);

        assertNotNull(annotation);
        assertEquals(LogModule.FOLLOW_UP_PLAN, annotation.module());
        assertEquals(LogType.UPDATE, annotation.type());
        assertEquals("{#request.id}", annotation.resourceId());
    }

    @Test
    void statusChangeWritesLogSnapshot() {
        service.updateStatus(statusRequest(FollowUpPlanStatusType.COMPLETED.name()), USER, ORG);

        verify(plans).update(any(FollowUpPlan.class));
        LogContextInfo context = OperationLogContext.getContext();
        assertNotNull(context, "状态变化必须写入日志上下文, 否则切面会因为没有资源名而丢弃日志");
        assertEquals(CUSTOMER_NAME, context.getResourceName());
        assertEquals(FollowUpPlanStatusType.PREPARED.name(), ((FollowUpPlan) context.getOriginalValue()).getStatus());
        assertEquals(FollowUpPlanStatusType.COMPLETED.name(), ((FollowUpPlan) context.getModifiedValue()).getStatus());
    }

    @Test
    void statusWithoutRelatedResourceFallsBackToFixedResourceName() {
        when(base.getCustomerMap(anyList())).thenReturn(Map.of());

        service.updateStatus(statusRequest(FollowUpPlanStatusType.UNDERWAY.name()), USER, ORG);

        LogContextInfo context = OperationLogContext.getContext();
        assertNotNull(context);
        assertEquals("update_follow_up_plan", context.getResourceName());
    }

    @Test
    void unchangedStatusWritesNoLog() {
        service.updateStatus(statusRequest(FollowUpPlanStatusType.PREPARED.name()), USER, ORG);

        assertNull(OperationLogContext.getContext(), "状态没变不应产生操作日志");
    }

    @Test
    void blankStatusWritesNoLog() {
        service.updateStatus(statusRequest(null), USER, ORG);

        assertNull(OperationLogContext.getContext());
    }

    @Test
    void convertedCompletedPlanKeepsStatusAndLogsNothing() {
        plan.setStatus(FollowUpPlanStatusType.COMPLETED.name());
        plan.setConverted(true);

        service.updateStatus(statusRequest(FollowUpPlanStatusType.UNDERWAY.name()), USER, ORG);

        assertNull(OperationLogContext.getContext());
        verify(plans, never()).update(any(FollowUpPlan.class));
        assertEquals(FollowUpPlanStatusType.COMPLETED.name(), plan.getStatus());
    }

    private FollowUpPlanStatusRequest statusRequest(String status) {
        FollowUpPlanStatusRequest request = new FollowUpPlanStatusRequest();
        request.setId(PLAN_ID);
        request.setStatus(status);
        return request;
    }
}
