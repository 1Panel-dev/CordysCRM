package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 合并客户同样要过审批状态权限: 合并会删掉被合并的客户、还会改掉合并目标的负责人并接管它的
 * 联系人/商机/跟进, 两边都是一次合并操作。之前这里没校验, 各审批状态都把合并权限勾掉也拦不住。
 */
class CustomerMergeApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String TARGET_ID = "customer-target";
    private static final String OWNER = "owner-1";
    private static final List<String> MERGE_IDS = List.of("customer-1", "customer-2");

    private final CustomerService service = new CustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final CustomerContactService customerContactService = mock(CustomerContactService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerContact> customerContactMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> customerCollaborationMapper = mock(BaseMapper.class);
    private final CustomerCollaborationService customerCollaborationService = mock(CustomerCollaborationService.class);
    private final ExtCustomerContactMapper extCustomerContactMapper = mock(ExtCustomerContactMapper.class);
    private final ExtOpportunityMapper extOpportunityMapper = mock(ExtOpportunityMapper.class);
    private final ExtContractMapper extContractMapper = mock(ExtContractMapper.class);
    private final ExtOrderMapper extOrderMapper = mock(ExtOrderMapper.class);
    private final ExtFollowUpRecordMapper extFollowUpRecordMapper = mock(ExtFollowUpRecordMapper.class);
    private final ExtFollowUpPlanMapper extFollowUpPlanMapper = mock(ExtFollowUpPlanMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CustomerMergeRequest request = new CustomerMergeRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 合并日志与错误提示要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "customerContactService", customerContactService);
        ReflectionTestUtils.setField(service, "customerContactMapper", customerContactMapper);
        ReflectionTestUtils.setField(service, "customerCollaborationMapper", customerCollaborationMapper);
        ReflectionTestUtils.setField(service, "customerCollaborationService", customerCollaborationService);
        ReflectionTestUtils.setField(service, "extCustomerContactMapper", extCustomerContactMapper);
        ReflectionTestUtils.setField(service, "extOpportunityMapper", extOpportunityMapper);
        ReflectionTestUtils.setField(service, "extContractMapper", extContractMapper);
        ReflectionTestUtils.setField(service, "extOrderMapper", extOrderMapper);
        ReflectionTestUtils.setField(service, "extFollowUpRecordMapper", extFollowUpRecordMapper);
        ReflectionTestUtils.setField(service, "extFollowUpPlanMapper", extFollowUpPlanMapper);
        ReflectionTestUtils.setField(service, "logService", logService);

        // 合并目标与待合并客户的负责人一致, 绕过负责人变更那一段, 用例只关心权限校验
        request.setMergeIds(new ArrayList<>(List.of(TARGET_ID, "customer-1", "customer-2")));
        request.setToMergeId(TARGET_ID);
        request.setOwnerId(OWNER);

        when(customerMapper.selectByPrimaryKey(TARGET_ID)).thenReturn(customer(TARGET_ID));
        when(customerMapper.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.stream().map(this::customer).toList();
        });
        // 姓名与电话都没开查重, 联系人全并过去
        when(customerContactService.getUniqueMap(ORG_ID)).thenReturn(Map.of(
                BusinessModuleField.CUSTOMER_CONTACT_NAME.getKey(), false,
                BusinessModuleField.CUSTOMER_CONTACT_PHONE.getKey(), false));
        // 状态权限默认放行全部, 让其余用例只关心合并本身; 收窄权限的用例自己再覆盖一次
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> idsOf(invocation.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void statusPermissionIsCheckedOverTheMergeTargetAndTheMergedCustomersBeforeAnyWrite() {
        service.merge(request, USER_ID, ORG_ID);

        // 合并目标也在校验范围内: 它同样被改负责人、被塞进联系人, 漏掉它就等于留了个后门
        ArgumentCaptor<List<Customer>> captor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = inOrder(approvalFlowService, extCustomerContactMapper, customerMapper, logService);
        inOrder.verify(approvalFlowService).filterResourcesWithPermission(
                eq(ApprovalFormTypeEnum.CUSTOMER.getValue()), captor.capture(),
                eq(PermissionConstants.CUSTOMER_MANAGEMENT_MERGE), eq(ORG_ID), any(), any());
        // 校验要早于任何写操作: 联系人/商机/跟进的搬运, 以及最后删掉被合并的客户
        inOrder.verify(extCustomerContactMapper).batchMerge(any(), anyString(), anyString(), any(), any());
        inOrder.verify(customerMapper).deleteByIds(MERGE_IDS);
        assertEquals(List.of("customer-1", "customer-2", TARGET_ID), idsOf(captor.getValue()));
    }

    @Test
    void mergeIsRejectedWhenAnyInvolvedCustomerIsInAForbiddenStatus() {
        // 合并目标无权合并: 被合并的两个客户都是干净的, 但结果客户改不动, 整个合并就该拒绝
        doReturn(List.of("customer-1", "customer-2")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        GenericException exception = assertThrows(GenericException.class,
                () -> service.merge(request, USER_ID, ORG_ID));
        // 提示要指向"这几个客户里有的合不了", 而不是笼统的没有操作权限 —— 用户得知道去重挑
        assertEquals("no.customer.merge.permission", exception.getMessage());

        // 与批量操作不同: 无权的那几个不能被跳过接着并剩下的, 否则合并结果少几个客户是无声无息的
        verify(customerMapper, never()).deleteByIds(anyList());
        verify(extCustomerContactMapper, never()).batchMerge(any(), anyString(), anyString(), any(), any());
        verify(extOpportunityMapper, never()).batchMerge(any(), anyString(), anyString());
        verify(extContractMapper, never()).batchMerge(any(), anyString(), anyString());
        verify(extOrderMapper, never()).batchMerge(any(), anyString(), anyString());
        verify(extFollowUpRecordMapper, never()).batchMerge(any(), anyString(), anyString());
        verify(extFollowUpPlanMapper, never()).batchMerge(any(), anyString(), anyString());
        verify(logService, never()).batchAdd(anyList());
    }

    @Test
    void mergeWithoutAnyStatusRestrictionStillGoesThrough() {
        // 没配状态权限(或全部放行)时行为不变, 免得这条校验把正常合并也拦了
        assertDoesNotThrow(() -> service.merge(request, USER_ID, ORG_ID));

        verify(customerMapper).deleteByIds(MERGE_IDS);
        verify(logService).batchAdd(anyList());
    }

    private List<String> idsOf(List<Customer> customers) {
        return customers.stream().map(Customer::getId).toList();
    }

    private Customer customer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("customer-" + id);
        customer.setOwner(OWNER);
        return customer;
    }
}
