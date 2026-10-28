package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerBatchToPoolApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String OWNER = "owner-1";

    private final CustomerService service = new CustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final CustomerPoolService customerPoolService = mock(CustomerPoolService.class);
    private final CustomerContactService customerContactService = mock(CustomerContactService.class);
    private final CustomerOwnerHistoryService ownerHistoryService = mock(CustomerOwnerHistoryService.class);
    private final ExtCustomerMapper extCustomerMapper = mock(ExtCustomerMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
    private final BatchPoolReasonRequest request = new BatchPoolReasonRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 回收日志与错误提示要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "customerPoolService", customerPoolService);
        ReflectionTestUtils.setField(service, "customerContactService", customerContactService);
        ReflectionTestUtils.setField(service, "customerOwnerHistoryService", ownerHistoryService);
        ReflectionTestUtils.setField(service, "extCustomerMapper", extCustomerMapper);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSendService);
        // 状态权限默认放行全部, 让其余用例只关心回收本身; 收窄权限的用例自己再覆盖一次
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> permittedIds((List<Customer>) invocation.getArgument(1)));
        // 未指定目标公海时按负责人的默认公海回收
        when(customerPoolService.getOwnersDefaultPoolMap(any(), any()))
                .thenReturn(Map.of(OWNER, pool("pool-1", "默认公海")));

        request.setIds(List.of("customer-1", "customer-2"));
        request.setReasonId("reason-1");
    }

    private static List<String> permittedIds(List<Customer> customers) {
        return customers.stream().map(Customer::getId).toList();
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void statusPermissionIsCheckedBeforeTheRecycleWrites() {
        List<Customer> customers = List.of(customer("customer-1"), customer("customer-2"));
        when(customerMapper.selectByIds(request.getIds())).thenReturn(customers);

        service.batchToPool(request, USER_ID, ORG_ID);

        // 与其他批量操作一致: 先按审批状态过滤出有权回收的客户, 再去写回收 SQL
        InOrder inOrder = inOrder(approvalFlowService, extCustomerMapper);
        inOrder.verify(approvalFlowService).filterResourcesWithPermission(
                eq(ApprovalFormTypeEnum.CUSTOMER.getValue()), same(customers),
                eq(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE), eq(ORG_ID), any(), any());
        inOrder.verify(extCustomerMapper, times(2)).moveToPool(any(Customer.class));
    }

    @Test
    void customersTheOperatorHasNoPermissionForAreNotMoved() {
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        // doReturn 而非 when: 默认 stub 已注册, when(...) 会先跑一遍它, 拿到的却是匹配器占位的 null
        doReturn(List.of("customer-1")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        // 无权回收的客户不能跟着一起被移入公海, 负责人也不该被清空
        verify(extCustomerMapper).moveToPool(argThat(customer -> "customer-1".equals(customer.getId())));
        verify(extCustomerMapper, never()).moveToPool(argThat(customer -> "customer-2".equals(customer.getId())));
        verify(customerContactService, never()).updateContactOwner(eq("customer-2"), anyString(), anyString(),
                anyString());
        // 没权限的那些既不算成功也不跳过, 与查不到的 id 一样落在 fail 里
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    @Test
    void recycleWithoutAnyPermittedCustomerTouchesNothing() {
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(customer("customer-1"), customer("customer-2")));
        doReturn(List.of()).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        verify(extCustomerMapper, never()).moveToPool(any(Customer.class));
        verify(ownerHistoryService, never()).add(any(Customer.class), anyString(), anyBoolean());
        verify(logService, never()).batchAdd(anyList());
        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
    }

    @Test
    void customersAlreadyInThePoolAreSkippedAndCountedAsFail() {
        Customer inPool = customer("customer-1");
        inPool.setInSharedPool(true);
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(inPool, customer("customer-2")));

        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        verify(extCustomerMapper, never()).moveToPool(argThat(customer -> "customer-1".equals(customer.getId())));
        verify(extCustomerMapper).moveToPool(argThat(customer -> "customer-2".equals(customer.getId())));
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    private Customer customer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("customer-" + id);
        customer.setOwner(OWNER);
        return customer;
    }

    private CustomerPool pool(String id, String name) {
        CustomerPool pool = new CustomerPool();
        pool.setId(id);
        pool.setName(name);
        pool.setOrganizationId(ORG_ID);
        return pool;
    }
}
