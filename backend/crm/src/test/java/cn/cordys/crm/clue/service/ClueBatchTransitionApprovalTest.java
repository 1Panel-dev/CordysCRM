package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.request.BatchReTransitionCustomerRequest;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClueBatchTransitionApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String OWNER = "owner-1";
    private static final String CUSTOMER_ID = "customer-1";

    private ClueService service;

    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final PoolCustomerService poolCustomerService = mock(PoolCustomerService.class);
    private final BatchReTransitionCustomerRequest request = new BatchReTransitionCustomerRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 用 spy 是为了把真正改数据的 transitionCs 短路掉, 只验证哪些线索被放行
        service = spy(new ClueService());
        // 线索ID为空时取翻译报错, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "poolCustomerService", poolCustomerService);
        doReturn(true).when(service).transitionCs(any(), any(), any(), any());
        // 状态权限默认放行全部, 让其余用例只关心关联本身; 收窄权限的用例自己再覆盖一次
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> permittedIds(invocation.getArgument(1)));

        request.setClueIds(List.of("clue-1", "clue-2"));
        request.setCustomerId(CUSTOMER_ID);
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @SuppressWarnings("unchecked")
    private static List<String> permittedIds(List<Clue> clues) {
        return clues.stream().map(Clue::getId).toList();
    }

    @Test
    void statusPermissionIsCheckedBeforeTheCustomerIsPicked() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customerInPool());

        service.batchTransition(request, USER_ID, ORG_ID);

        // 与其他批量操作一致: 先按审批状态过滤出有权编辑的线索, 再去领取公海客户
        InOrder inOrder = inOrder(approvalFlowService, poolCustomerService);
        inOrder.verify(approvalFlowService).filterResourcesWithPermission(
                eq(ApprovalFormTypeEnum.CLUE.getValue()), any(),
                eq(PermissionConstants.CLUE_MANAGEMENT_UPDATE), eq(ORG_ID), any(), any());
        inOrder.verify(poolCustomerService).pick(any(), eq(USER_ID), eq(ORG_ID));
    }

    @Test
    void cluesTheOperatorHasNoPermissionForAreNotAssociated() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customerInPool());
        // doReturn 而非 when: 默认 stub 已注册, when(...) 会先跑一遍它, 拿到的却是匹配器占位的 null
        doReturn(List.of("clue-1")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        service.batchTransition(request, USER_ID, ORG_ID);

        // 无权编辑的线索不能跟着一起被关联到客户
        verify(service).transitionCs(eq("clue-1"), any(Customer.class), eq(USER_ID), eq(ORG_ID));
        verify(service, never()).transitionCs(eq("clue-2"), any(Customer.class), any(), any());
    }

    @Test
    void associateWithoutAnyPermittedClueTouchesNothing() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        doReturn(List.of()).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectReasonResponse response = service.batchTransition(request, USER_ID, ORG_ID);

        // 一条都无权关联时不该再读客户, 更不该把客户从公海里白领走
        verify(customerMapper, never()).selectByPrimaryKey(any());
        verify(poolCustomerService, never()).pick(any(), any(), any());
        verify(service, never()).transitionCs(any(), any(), any(), any());
        // 整批都算失败
        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
    }

    @Test
    void associateReturnsSuccessAndFailCounts() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customerInPool());
        doReturn(List.of("clue-1")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectReasonResponse response = service.batchTransition(request, USER_ID, ORG_ID);

        // 关联上的 1 条算成功, 无权关联的另 1 条算失败
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    @Test
    void cluesThatFailToAssociateAreCountedAsFail() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customerInPool());
        // 负责人不存在时 transitionCs 返回 false, 这类线索不该被算成成功
        doReturn(false).when(service).transitionCs(any(), any(), any(), any());

        BatchAffectReasonResponse response = service.batchTransition(request, USER_ID, ORG_ID);

        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
    }

    @Test
    void missingCustomerFailsWholeBatch() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(null);

        BatchAffectReasonResponse response = service.batchTransition(request, USER_ID, ORG_ID);

        // 客户都查不到时不该把线索关联到一个空客户上
        verify(service, never()).transitionCs(any(), any(), any(), any());
        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
    }

    private Clue clue(String id) {
        Clue clue = new Clue();
        clue.setId(id);
        clue.setName("clue-" + id);
        clue.setOwner(OWNER);
        return clue;
    }

    private Customer customerInPool() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("customer-1");
        customer.setOwner(OWNER);
        customer.setInSharedPool(true);
        customer.setPoolId("pool-1");
        return customer;
    }
}
