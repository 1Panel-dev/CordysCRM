package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.domain.OrderSnapshot;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 合并客户时, 被合并客户名下的合同/订单只存了 customer_id: 客户删掉后列表里的客户名会变成空值。
 * 这里锁住两件事: 合同/订单在客户删除前改挂到合并后的客户, 且各自留下一条修改日志。
 */
class CustomerMergeRelatedResourceTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String TARGET_ID = "customer-target";
    private static final String MERGED_ID = "customer-merged";
    private static final String OWNER = "owner-1";

    private final CustomerService service = new CustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final CustomerContactService customerContactService = mock(CustomerContactService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerContact> customerContactMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> customerCollaborationMapper = mock(BaseMapper.class);
    private final ExtCustomerContactMapper extCustomerContactMapper = mock(ExtCustomerContactMapper.class);
    private final ExtOpportunityMapper extOpportunityMapper = mock(ExtOpportunityMapper.class);
    private final ExtContractMapper extContractMapper = mock(ExtContractMapper.class);
    private final ExtOrderMapper extOrderMapper = mock(ExtOrderMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ContractSnapshot> contractSnapshotMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<OrderSnapshot> orderSnapshotMapper = mock(BaseMapper.class);
    private final ExtFollowUpRecordMapper extFollowUpRecordMapper = mock(ExtFollowUpRecordMapper.class);
    private final ExtFollowUpPlanMapper extFollowUpPlanMapper = mock(ExtFollowUpPlanMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CustomerMergeRequest request = new CustomerMergeRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "customerContactService", customerContactService);
        ReflectionTestUtils.setField(service, "customerContactMapper", customerContactMapper);
        ReflectionTestUtils.setField(service, "customerCollaborationMapper", customerCollaborationMapper);
        ReflectionTestUtils.setField(service, "extCustomerContactMapper", extCustomerContactMapper);
        ReflectionTestUtils.setField(service, "extOpportunityMapper", extOpportunityMapper);
        ReflectionTestUtils.setField(service, "extContractMapper", extContractMapper);
        ReflectionTestUtils.setField(service, "extOrderMapper", extOrderMapper);
        ReflectionTestUtils.setField(service, "contractSnapshotMapper", contractSnapshotMapper);
        ReflectionTestUtils.setField(service, "orderSnapshotMapper", orderSnapshotMapper);
        ReflectionTestUtils.setField(service, "extFollowUpRecordMapper", extFollowUpRecordMapper);
        ReflectionTestUtils.setField(service, "extFollowUpPlanMapper", extFollowUpPlanMapper);
        ReflectionTestUtils.setField(service, "logService", logService);

        // 负责人不变, 用例只关心合同/订单的归属搬运
        request.setMergeIds(new ArrayList<>(List.of(TARGET_ID, MERGED_ID)));
        request.setToMergeId(TARGET_ID);
        request.setOwnerId(OWNER);

        when(customerMapper.selectByPrimaryKey(TARGET_ID)).thenReturn(customer(TARGET_ID));
        when(customerMapper.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.stream().map(this::customer).toList();
        });
        when(customerContactService.getUniqueMap(ORG_ID)).thenReturn(Map.of(
                BusinessModuleField.CUSTOMER_CONTACT_NAME.getKey(), false,
                BusinessModuleField.CUSTOMER_CONTACT_PHONE.getKey(), false));
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> idsOf(invocation.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void contractsAndOrdersMoveToTheTargetCustomerBeforeTheMergedCustomerIsDeleted() {
        service.merge(request, USER_ID, ORG_ID);

        InOrder inOrder = inOrder(extContractMapper, extOrderMapper, customerMapper);
        inOrder.verify(extContractMapper).batchMerge(request, USER_ID, ORG_ID);
        inOrder.verify(extOrderMapper).batchMerge(request, USER_ID, ORG_ID);
        inOrder.verify(customerMapper).deleteByIds(List.of(MERGED_ID));
    }

    @Test
    void contractAndOrderCustomerChangesAreLogged() {
        Contract contract = contract("contract-1", MERGED_ID);
        Order order = order("order-1", MERGED_ID);
        when(extContractMapper.getMergeContractList(any(), eq(ORG_ID))).thenReturn(List.of(contract));
        when(extOrderMapper.getMergeOrderList(any(), eq(ORG_ID))).thenReturn(List.of(order));

        service.merge(request, USER_ID, ORG_ID);

        ArgumentCaptor<List<LogDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(logService).batchAdd(captor.capture());
        List<LogDTO> logs = captor.getValue();

        LogDTO contractLog = findLog(logs, LogModule.CONTRACT_INDEX, contract.getId());
        assertEquals(contract.getName(), contractLog.getResourceName());
        assertEquals(customer(MERGED_ID).getName(), ((Contract) contractLog.getOriginalValue()).getCustomerId());
        assertEquals(TARGET_ID, ((Contract) contractLog.getModifiedValue()).getCustomerId());

        LogDTO orderLog = findLog(logs, LogModule.ORDER_INDEX, order.getId());
        assertEquals(order.getName(), orderLog.getResourceName());
        assertEquals(customer(MERGED_ID).getName(), ((Order) orderLog.getOriginalValue()).getCustomerId());
        assertEquals(TARGET_ID, ((Order) orderLog.getModifiedValue()).getCustomerId());
    }

    @Test
    void contractAndOrderSnapshotsAreMovedToTheTargetCustomer() {
        Contract contract = contract("contract-1", MERGED_ID);
        Order order = order("order-1", MERGED_ID);
        when(extContractMapper.getMergeContractList(any(), eq(ORG_ID))).thenReturn(List.of(contract));
        when(extOrderMapper.getMergeOrderList(any(), eq(ORG_ID))).thenReturn(List.of(order));
        ContractSnapshot contractSnapshot = contractSnapshot(contract.getId());
        OrderSnapshot orderSnapshot = orderSnapshot(order.getId());
        when(contractSnapshotMapper.selectListByLambda(any())).thenReturn(List.of(contractSnapshot));
        when(orderSnapshotMapper.selectListByLambda(any())).thenReturn(List.of(orderSnapshot));

        service.merge(request, USER_ID, ORG_ID);

        ContractGetResponse contractValue = JSON.parseObject(contractSnapshot.getContractValue(),
                ContractGetResponse.class);
        assertEquals(TARGET_ID, contractValue.getCustomerId());
        assertEquals(customer(TARGET_ID).getName(), contractValue.getCustomerName());
        assertEquals(TARGET_ID,
                contractValue.getOptionMap().get(BusinessModuleField.CONTRACT_CUSTOMER_NAME.getBusinessKey())
                        .getFirst().getId());
        verify(contractSnapshotMapper).update(contractSnapshot);

        OrderGetResponse orderValue = JSON.parseObject(orderSnapshot.getOrderValue(), OrderGetResponse.class);
        assertEquals(TARGET_ID, orderValue.getCustomerId());
        assertEquals(customer(TARGET_ID).getName(), orderValue.getCustomerName());
        assertEquals(TARGET_ID,
                orderValue.getOptionMap().get(BusinessModuleField.ORDER_CUSTOMER.getBusinessKey())
                        .getFirst().getId());
        verify(orderSnapshotMapper).update(orderSnapshot);
    }

    private LogDTO findLog(List<LogDTO> logs, String module, String resourceId) {
        return logs.stream()
                .filter(log -> module.equals(log.getModule()) && resourceId.equals(log.getResourceId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing log: " + module + "/" + resourceId));
    }

    private ContractSnapshot contractSnapshot(String contractId) {
        ContractGetResponse response = new ContractGetResponse();
        response.setCustomerId(MERGED_ID);
        response.setCustomerName(customer(MERGED_ID).getName());
        response.setOptionMap(new HashMap<>(Map.of(
                BusinessModuleField.CONTRACT_CUSTOMER_NAME.getBusinessKey(),
                new ArrayList<>(List.of(new OptionDTO(MERGED_ID, customer(MERGED_ID).getName()))))));
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setId("snapshot-" + contractId);
        snapshot.setContractId(contractId);
        snapshot.setContractValue(JSON.toJSONString(response));
        return snapshot;
    }

    private OrderSnapshot orderSnapshot(String orderId) {
        OrderGetResponse response = new OrderGetResponse();
        response.setCustomerId(MERGED_ID);
        response.setCustomerName(customer(MERGED_ID).getName());
        response.setOptionMap(new HashMap<>(Map.of(
                BusinessModuleField.ORDER_CUSTOMER.getBusinessKey(),
                new ArrayList<>(List.of(new OptionDTO(MERGED_ID, customer(MERGED_ID).getName()))))));
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setId("snapshot-" + orderId);
        snapshot.setOrderId(orderId);
        snapshot.setOrderValue(JSON.toJSONString(response));
        return snapshot;
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

    private Contract contract(String id, String customerId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setName("contract-" + id);
        contract.setCustomerId(customerId);
        return contract;
    }

    private Order order(String id, String customerId) {
        Order order = new Order();
        order.setId(id);
        order.setName("order-" + id);
        order.setCustomerId(customerId);
        return order;
    }
}
