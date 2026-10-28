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
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * 客户删除的引用校验必须在提审之前: 切面命中删除审批流时会跳过方法体直接提审,
 * 校验若留到审批通过才跑, 审批人白跑一趟 (还会把审批实例卡在无法完成的节点上)。
 */
class CustomerDeleteApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";

    private final CustomerService service = spy(new CustomerService());
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ExtCustomerMapper extCustomerMapper = mock(ExtCustomerMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService commonNoticeSendService = mock(CommonNoticeSendService.class);
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 返回值要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "extCustomerMapper", extCustomerMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", commonNoticeSendService);
        doNothing().when(service).deleteCustomerResource(anyList());
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void deleteApprovalAnnotationSitsOnTheInnerMethodNotTheEntryPoint() throws Exception {
        HitApproval hitApproval = CustomerService.class
                .getMethod("deleteTriggerApproval", String.class, String.class, String.class)
                .getAnnotation(HitApproval.class);

        assertEquals(FormKey.CUSTOMER, hitApproval.formKey());
        assertEquals(ExecuteTimingEnum.DELETE, hitApproval.executeType());
        assertEquals("{#id}", hitApproval.resourceId());
        assertEquals("{#userId}", hitApproval.operatorId());
        // 注解若挪回 deleteWithApprovalCheck, 引用校验就又跑在切面后面了, 变回审批通过才报错
        assertNull(CustomerService.class
                .getMethod("deleteWithApprovalCheck", String.class, String.class, String.class)
                .getAnnotation(HitApproval.class));
    }

    @Test
    void singleDeleteChecksResourceRefBeforeHandingToTheApprovalEntry() {
        doNothing().when(service).checkResourceRef(anyList());
        doNothing().when(service).delete(anyString(), anyString(), anyString());
        doNothing().when(service).deleteTriggerApproval(anyString(), anyString(), anyString());

        runDelete("customer-1");

        InOrder inOrder = inOrder(service);
        inOrder.verify(service).checkResourceRef(List.of("customer-1"));
        inOrder.verify(service).deleteTriggerApproval("customer-1", USER_ID, ORG_ID);
    }

    @Test
    void singleDeleteWithAReferenceThrowsBeforeAnyApprovalIsRaised() {
        doThrow(new GenericException(CustomerResultCode.CUSTOMER_RESOURCE_REF))
                .when(service).checkResourceRef(List.of("customer-1"));

        assertThrows(GenericException.class, () -> runDelete("customer-1"));

        // 校验不过就直接中止, 审批入口一次都不进
        verify(service, never()).deleteTriggerApproval(anyString(), anyString(), anyString());
        verify(service, never()).delete(anyString(), anyString(), anyString());
    }

    @Test
    void batchDeleteChecksResourceRefOverEveryPermittedCustomerBeforeTheTrigger() {
        when(customerMapper.selectByIds(anyList()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of("customer-1", "customer-2"));
        when(approvalResourceService.batchDeleteTriggerApproval(anyList(), any(), anyString(), anyString(), anyMap()))
                .thenReturn(List.of("customer-2"));
        when(extCustomerMapper.hasRefOpportunity(anyList())).thenReturn(false);
        when(extCustomerMapper.hasRefContact(anyList())).thenReturn(false);

        runBatchDelete(List.of("customer-1", "customer-2"));

        // 校验要覆盖有权限的全部(含要进审批的 customer-2), 且必须早于提审
        InOrder inOrder = inOrder(service, approvalResourceService);
        inOrder.verify(service).checkResourceRef(List.of("customer-1", "customer-2"));
        inOrder.verify(approvalResourceService).batchDeleteTriggerApproval(
                anyList(), any(), anyString(), anyString(), anyMap());
    }

    @Test
    void batchDeleteWithAReferenceThrowsBeforeAnyApprovalIsRaised() {
        when(customerMapper.selectByIds(anyList()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of("customer-1", "customer-2"));
        doThrow(new GenericException(CustomerResultCode.CUSTOMER_RESOURCE_REF))
                .when(service).checkResourceRef(anyList());

        assertThrows(GenericException.class, () -> runBatchDelete(List.of("customer-1", "customer-2")));

        verify(approvalResourceService, never()).batchDeleteTriggerApproval(
                anyList(), any(), anyString(), anyString(), anyMap());
        verify(service, never()).deleteCustomerResource(anyList());
    }

    private void runDelete(String id) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(service);
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            service.deleteWithApprovalCheck(id, USER_ID, ORG_ID);
        }
    }

    private void runBatchDelete(List<String> ids) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            service.batchDelete(ids, USER_ID, ORG_ID);
        }
    }

    private Customer customer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("customer-" + id);
        customer.setOwner("owner-1");
        return customer;
    }
}
