package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.customer.constants.CustomerResultCode;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公海客户接入审批流: 批量编辑走编辑审批、删除/批量删除走删除审批, 领取与分配不接审批。
 */
class PoolCustomerApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String FIELD_ID = "field-1";

    private final PoolCustomerService service = new PoolCustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ExtCustomerMapper extCustomerMapper = mock(ExtCustomerMapper.class);
    private final CustomerFieldService customerFieldService = mock(CustomerFieldService.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final CustomerService customerService = mock(CustomerService.class);
    private final LogService logService = mock(LogService.class);
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 返回值要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "extCustomerMapper", extCustomerMapper);
        ReflectionTestUtils.setField(service, "customerFieldService", customerFieldService);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "logService", logService);
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void deleteApprovalAnnotationSitsOnTheInnerMethodNotTheEntryPoint() throws Exception {
        HitApproval hitApproval = PoolCustomerService.class
                .getMethod("deleteTriggerApproval", String.class, String.class, String.class)
                .getAnnotation(HitApproval.class);

        assertEquals(FormKey.CUSTOMER, hitApproval.formKey());
        assertEquals(ExecuteTimingEnum.DELETE, hitApproval.executeType());
        assertEquals("{#id}", hitApproval.resourceId());
        assertEquals("{#userId}", hitApproval.operatorId());
        // 注解若挪回 deleteWithApprovalCheck, 引用校验就又跑在切面后面了, 变成审批通过才报错
        assertNull(PoolCustomerService.class
                .getMethod("deleteWithApprovalCheck", String.class, String.class, String.class)
                .getAnnotation(HitApproval.class));
        // 触发点必须留在入口上, 原方法不能重复触发
        assertNull(PoolCustomerService.class.getMethod("delete", String.class).getAnnotation(HitApproval.class));
    }

    @Test
    void singleDeleteChecksResourceRefBeforeHandingToTheApprovalEntry() {
        PoolCustomerService spy = spy(service);
        doNothing().when(spy).delete(anyString());

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(customerService);
            beanFactory.when(() -> CommonBeanFactory.getBean(PoolCustomerService.class)).thenReturn(spy);
            spy.deleteWithApprovalCheck("customer-1", USER_ID, ORG_ID);
        }

        // 校验先跑, 再委托
        InOrder inOrder = inOrder(customerService, spy);
        inOrder.verify(customerService).checkResourceRef(List.of("customer-1"));
        inOrder.verify(spy).deleteTriggerApproval("customer-1", USER_ID, ORG_ID);
    }

    @Test
    void singleDeleteAbortsBeforeApprovalWhenTheCustomerIsStillReferenced() {
        PoolCustomerService spy = spy(service);
        doThrow(new GenericException(CustomerResultCode.CUSTOMER_RESOURCE_REF))
                .when(customerService).checkResourceRef(List.of("customer-1"));

        try (MockedStatic<CommonBeanFactory> mockFactory = mockStatic(CommonBeanFactory.class)) {
            mockFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(customerService);
            mockFactory.when(() -> CommonBeanFactory.getBean(PoolCustomerService.class)).thenReturn(spy);
            // 校验不过就直接抛, 审批入口一次都不进 —— 提审前中止
            assertThrows(GenericException.class, () -> spy.deleteWithApprovalCheck("customer-1", USER_ID, ORG_ID));
        }

        verify(spy, never()).deleteTriggerApproval(anyString(), anyString(), anyString());
        verify(spy, never()).delete(anyString());
    }

    @Test
    void batchEditOnANonOwnerFieldTriggersTheEditApproval() {
        when(customerFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("remark", "备注"));
        when(customerMapper.selectByIds(anyList())).thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("customer-1"));

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("customer-1", "customer-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        // 只有有权限的那条被提审, 也只有它被写库
        verify(approvalResourceService).batchEditTriggerApproval(
                eq(List.of("customer-1")), eq(FIELD_ID), eq(FormKey.CUSTOMER), eq(ORG_ID), eq(USER_ID), eq("备注"),
                eq("value"));
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
        assertEquals(0, response.getSkip());
    }

    @Test
    void batchEditOnTheOwnerFieldGoesToBatchAssignWithoutApproval() {
        when(customerFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("owner", "负责人"));
        when(customerMapper.selectByIds(anyList())).thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("customer-1", "customer-2"));

        PoolCustomerService spy = spy(service);
        doNothing().when(spy).batchAssign(any(), anyString(), anyString(), anyString());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("customer-1", "customer-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("new-owner");

        BatchAffectReasonResponse response;
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            response = spy.batchUpdate(request, USER_ID, ORG_ID);
        }

        // 改负责人就是一次分配, 分配不接审批
        ArgumentCaptor<PoolBatchAssignRequest> assignRequest = ArgumentCaptor.forClass(PoolBatchAssignRequest.class);
        verify(spy).batchAssign(assignRequest.capture(), eq("new-owner"), eq(ORG_ID), eq(USER_ID));
        assertEquals(List.of("customer-1", "customer-2"), assignRequest.getValue().getBatchIds());
        verify(approvalResourceService, never()).batchEditTriggerApproval(
                anyList(), anyString(), any(), anyString(), anyString(), anyString(), any());
        assertEquals(2, response.getSuccess());
        assertEquals(0, response.getFail());
    }

    @Test
    void batchEditWithoutAnyPermittedResourceDoesNotWrite() {
        when(customerFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("remark", "备注"));
        when(customerMapper.selectByIds(anyList())).thenReturn(List.of(customer("customer-1")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("customer-1"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        verify(approvalResourceService, never()).batchEditTriggerApproval(
                anyList(), anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(customerFieldService, never()).batchUpdate(
                any(), any(), anyList(), any(), anyString(), any(), anyString(), anyString(), eq(true));
        assertEquals(0, response.getSuccess());
        assertEquals(1, response.getFail());
        assertEquals("no.operation.permission", response.getErrorMessages());
    }

    @Test
    void batchEditOnUnknownIdsReportsThemAsFailed() {
        when(customerFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("remark", "备注"));
        when(customerMapper.selectByIds(anyList())).thenReturn(List.of());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("customer-1", "customer-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
        verify(approvalFlowService, never()).filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void batchDeleteSkipsTheResourcesThatWerePushedToApproval() {
        when(customerMapper.selectByIds(anyList()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2"), customer("customer-3")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("customer-1", "customer-2"));
        when(approvalResourceService.batchDeleteTriggerApproval(anyList(), any(), anyString(), anyString(), anyMap()))
                .thenReturn(List.of("customer-2"));

        BatchAffectReasonResponse response = runDelete(List.of("customer-1", "customer-2", "customer-3"));

        // 进了审批的 customer-2 不能被真删掉
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(1, response.getFail());
        assertEquals("batch.operate.reason", response.getErrorMessages());
        verify(approvalResourceService).batchDeleteTriggerApproval(
                eq(List.of("customer-1", "customer-2")), eq(FormKey.CUSTOMER), eq(ORG_ID), eq(USER_ID),
                argThat(nameMap -> nameMap.size() == 2 && "customer-customer-1".equals(nameMap.get("customer-1"))));
        // 引用校验要覆盖有权限的全部(含要进审批的), 且必须早于提审 —— 否则那两条要等审批通过才报错
        InOrder inOrder = inOrder(customerService, approvalResourceService);
        inOrder.verify(customerService).checkResourceRef(List.of("customer-1", "customer-2"));
        inOrder.verify(approvalResourceService).batchDeleteTriggerApproval(
                anyList(), any(), anyString(), anyString(), anyMap());
    }

    @Test
    void batchDeleteAbortsBeforeApprovalWhenAnyCustomerIsStillReferenced() {
        when(customerMapper.selectByIds(anyList()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("customer-1", "customer-2"));
        doThrow(new GenericException(CustomerResultCode.CUSTOMER_RESOURCE_REF))
                .when(customerService).checkResourceRef(anyList());

        assertThrows(GenericException.class, () -> runDelete(List.of("customer-1", "customer-2")));

        // 一条审批都不发, 也不真删
        verify(approvalResourceService, never()).batchDeleteTriggerApproval(
                anyList(), any(), anyString(), anyString(), anyMap());
        verify(customerService, never()).deleteCustomerResource(anyList());
    }

    @Test
    void batchDeleteWithoutAnyPermittedResourceDoesNotDelete() {
        when(customerMapper.selectByIds(anyList())).thenReturn(List.of(customer("customer-1")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of());

        BatchAffectReasonResponse response = runDelete(List.of("customer-1"));

        verify(approvalResourceService, never()).batchDeleteTriggerApproval(
                anyList(), any(), anyString(), anyString(), anyMap());
        assertEquals(0, response.getSuccess());
        assertEquals(1, response.getFail());
        assertEquals("no.operation.permission", response.getErrorMessages());
    }

    /**
     * 审批通过后的删除由框架落到 {@link CustomerService#delete}, 公海客户没有负责人,
     * 通知的收件人不能是 List.of(null)。
     */
    @Test
    void customerDeleteWithNoOwnerNotifiesNobodyInsteadOfThrowing() {
        CustomerService realService = spy(new CustomerService());
        CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(realService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(realService, "commonNoticeSendService", noticeSendService);
        doNothing().when(realService).checkResourceRef(anyList());
        doNothing().when(realService).deleteCustomerResource(anyList());
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(customer("customer-1"));

        realService.delete("customer-1", USER_ID, ORG_ID);

        verify(noticeSendService).sendNotice(
                eq(NotificationConstants.Module.CUSTOMER),
                eq(NotificationConstants.Event.CUSTOMER_DELETED),
                eq("customer-customer-1"),
                eq(USER_ID),
                eq(ORG_ID),
                eq(List.of()),
                eq(true));
    }

    private BatchAffectReasonResponse runEdit(ResourceBatchEditRequest request) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.batchUpdate(request, USER_ID, ORG_ID);
        }
    }

    private BatchAffectReasonResponse runDelete(List<String> ids) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(customerService);
            return service.batchDelete(ids, USER_ID, ORG_ID);
        }
    }

    private BaseField field(String businessKey, String name) {
        InputField field = new InputField();
        field.setId(FIELD_ID);
        field.setBusinessKey(businessKey);
        field.setName(name);
        return field;
    }

    private Customer customer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("customer-" + id);
        return customer;
    }
}
