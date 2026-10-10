package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalResourceUpdateType;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.request.CustomerApprovalSnapshotRequest;
import cn.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CustomerTransferApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String NEW_OWNER = "new-owner";

    private final CustomerService service = new CustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final PoolCustomerService poolCustomerService = mock(PoolCustomerService.class);
    private final CustomerOwnerHistoryService ownerHistoryService = mock(CustomerOwnerHistoryService.class);
    private final ExtCustomerMapper extCustomerMapper = mock(ExtCustomerMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final CustomerContactService customerContactService = mock(CustomerContactService.class);
    private final CustomerFieldService customerFieldService = mock(CustomerFieldService.class);
    private final StatisticFieldService statisticFieldService = mock(StatisticFieldService.class);
    private final ModuleFormService moduleFormService = mock(ModuleFormService.class);
    private final ModuleFormCacheService moduleFormCacheService = mock(ModuleFormCacheService.class);
    private final BaseService baseService = mock(BaseService.class);
    private final CustomerBatchTransferRequest request = new CustomerBatchTransferRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 转移/批量编辑的返回值要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "poolCustomerService", poolCustomerService);
        ReflectionTestUtils.setField(service, "customerOwnerHistoryService", ownerHistoryService);
        ReflectionTestUtils.setField(service, "extCustomerMapper", extCustomerMapper);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSendService);
        ReflectionTestUtils.setField(service, "customerContactService", customerContactService);
        ReflectionTestUtils.setField(service, "customerFieldService", customerFieldService);
        ReflectionTestUtils.setField(service, "statisticFieldService", statisticFieldService);
        ReflectionTestUtils.setField(service, "moduleFormService", moduleFormService);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", moduleFormCacheService);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        request.setIds(List.of("customer-1", "customer-2"));
        request.setOwner(NEW_OWNER);
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void onlyCustomersWhoseOwnerChangesArePushedToApproval() {
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(customer("customer-1", "old-owner"), customer("customer-2", NEW_OWNER)));

        runTransfer();

        // 转移 SQL 带 owner != #{owner}, 这批客户不会被改写, 也就不该凭空多出一条审批
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("customer-1"), BusinessModuleField.CUSTOMER_OWNER, FormKey.CUSTOMER, ORG_ID, USER_ID, NEW_OWNER);
    }

    @Test
    void snapshotIsTakenBeforeTheTransferWrites() {
        when(customerMapper.selectByIds(request.getIds())).thenReturn(List.of(customer("customer-1", "old-owner")));

        runTransfer();

        // 快照必须早于转移 SQL, 否则驳回/撤回会把负责人回退成转移后的值
        InOrder inOrder = inOrder(approvalResourceService, extCustomerMapper);
        inOrder.verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("customer-1"), BusinessModuleField.CUSTOMER_OWNER, FormKey.CUSTOMER, ORG_ID, USER_ID, NEW_OWNER);
        inOrder.verify(extCustomerMapper).batchTransfer(request, USER_ID);
    }

    @Test
    void transferThatKeepsEveryOwnerStillGoesThroughTheEntryPointWithNoResource() {
        when(customerMapper.selectByIds(request.getIds())).thenReturn(List.of(customer("customer-1", NEW_OWNER)));

        runTransfer();

        // 入口必须被调用, 由它自己短路; 若改成调用方提前 return, 这里的空集合断言会失败
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of(), BusinessModuleField.CUSTOMER_OWNER, FormKey.CUSTOMER, ORG_ID, USER_ID, NEW_OWNER);
    }

    @Test
    void contactOwnerFollowsOnlyTheCustomersWhoseOwnerChanges() {
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(customer("customer-1", "old-owner"), customer("customer-2", NEW_OWNER)));

        runTransfer();

        // 与单个编辑一致: 只有负责人真的换掉的客户, 其联系人才跟着换人
        verify(customerContactService).updateContactOwner("customer-1", NEW_OWNER, "old-owner", ORG_ID);
        verify(customerContactService, never()).updateContactOwner(eq("customer-2"), anyString(), anyString(), anyString());
    }

    @Test
    void transferReportsChangedSkippedAndMissingCounts() {
        // 入参里带一个查不到的 id, 它既不算成功也不算跳过
        request.setIds(List.of("customer-1", "customer-2", "customer-3"));
        when(customerMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(customer("customer-1", "old-owner"), customer("customer-2", NEW_OWNER)));

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(1, response.getFail());
    }

    @Test
    void transferWithUnknownIdsOnlyReturnsNoResource() {
        when(customerMapper.selectByIds(request.getIds())).thenReturn(List.of());

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(0, response.getSuccess());
        verify(extCustomerMapper, never()).batchTransfer(any(), anyString());
    }

    @Test
    void revertRestoresTheCollectionTimeThatTheEditRevertResetsToNow() {
        Customer row = customer("customer-1", NEW_OWNER);
        row.setCollectionTime(2_000L);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);
        // 让 update() 的写回落到同一行上, 否则 selectByPrimaryKey 永远返回转移后的旧值, 看不出还原动作
        when(customerMapper.update(any(Customer.class))).thenAnswer(invocation -> {
            Customer written = invocation.getArgument(0);
            row.setOwner(written.getOwner());
            row.setCollectionTime(written.getCollectionTime());
            return 1;
        });

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        snapshot.setCollectionTime(1_000L);
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(service);
            service.revertToSnapshot("customer-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }

        // 编辑回退的负责人变更分支会把领取时间重置为回退时间, 转移前的领取时间必须按快照补回
        assertEquals("old-owner", row.getOwner());
        verify(extCustomerMapper).revertTransferByApproval("customer-1", 1_000L);
    }

    @Test
    void revertCleansUpTheTransferHistoryAndDoesNotRecordANewOne() {
        Customer row = customer("customer-1", NEW_OWNER);
        row.setCollectionTime(2_000L);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        snapshot.setCollectionTime(1_000L);
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(service);
            service.revertToSnapshot("customer-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }

        // 转移时 batchAdd 写的那条记录要按快照里的(负责人, 领取时间)删掉
        verify(ownerHistoryService).deleteTransferHistory("customer-1", "old-owner", 1_000L);
        // 回退本身不是一次负责人变更, 不该再往历史里写一条
        verify(ownerHistoryService, never()).add(any(Customer.class), anyString(), anyBoolean());
        // 把客户还给原负责人也不该受原负责人的容量限制, 否则异常会被 revertToSnapshot 吞掉, 数据原地不动
        verify(poolCustomerService, never()).validateCapacity(anyInt(), anyString(), anyString());
    }

    @Test
    void snapshotRecordsTheContactsThatTheOwnerChangeWillMove() {
        Customer row = customer("customer-1", "old-owner");
        row.setOrganizationId(ORG_ID);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);
        when(customerContactService.listOwnedContactIds("customer-1", "old-owner", ORG_ID))
                .thenReturn(List.of("contact-1", "contact-2"));

        String snapshotData = service.getPreUpdateSnapshotData("customer-1", USER_ID, ORG_ID);

        // 回退要按 id 精确还原, 因此快照必须记下当前挂在负责人名下的那批联系人
        assertEquals(List.of("contact-1", "contact-2"),
                JSON.parseObject(snapshotData, CustomerApprovalSnapshotRequest.class).getContactIds());
    }

    @Test
    void revertRestoresOnlyTheContactsThatTheOwnerChangeMoved() {
        Customer row = customer("customer-1", NEW_OWNER);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        snapshot.setContactIds(List.of("contact-1", "contact-2"));
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        runRevert(snapshot);

        // 按 id 还原, 不按负责人反查 —— 否则本来就挂在新负责人名下的联系人也一起被拖回原负责人
        verify(customerContactService).updateContactOwnerByIds("customer-1", List.of("contact-1", "contact-2"), "old-owner", ORG_ID);
        verify(customerContactService, never()).updateContactOwner(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void revertWithNoMovedContactTouchesNothing() {
        Customer row = customer("customer-1", NEW_OWNER);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        // 空集合是有效信息: 这次改派没有牵连任何联系人
        snapshot.setContactIds(List.of());
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        runRevert(snapshot);

        verify(customerContactService, never()).updateContactOwnerByIds(anyString(), anyList(), anyString(), anyString());
        verify(customerContactService, never()).updateContactOwner(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void revertFallsBackToTheOwnerLookupForSnapshotsTakenBeforeContactIdsExisted() {
        Customer row = customer("customer-1", NEW_OWNER);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        // 升级前落库的旧快照没有 contactIds
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        runRevert(snapshot);

        verify(customerContactService).updateContactOwner("customer-1", "old-owner", NEW_OWNER, ORG_ID);
        verify(customerContactService, never()).updateContactOwnerByIds(anyString(), anyList(), anyString(), anyString());
    }

    @Test
    void revertMovesTheContactsBackUnderTheRestoredOwner() {
        Customer row = customer("customer-1", NEW_OWNER);
        row.setCollectionTime(2_000L);
        when(customerMapper.selectByPrimaryKey("customer-1")).thenReturn(row);

        CustomerApprovalSnapshotRequest snapshot = new CustomerApprovalSnapshotRequest();
        snapshot.setId("customer-1");
        snapshot.setOwner("old-owner");
        snapshot.setCollectionTime(1_000L);
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        runRevert(snapshot);

        // 编辑回退复用 update(), 负责人从 NEW_OWNER 变回 old-owner, 联系人负责人要跟着走回来
        verify(customerContactService).updateContactOwner("customer-1", "old-owner", NEW_OWNER, ORG_ID);
    }

    private void runRevert(CustomerApprovalSnapshotRequest snapshot) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(CustomerService.class)).thenReturn(service);
            service.revertToSnapshot("customer-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }
    }

    private BatchAffectReasonResponse runTransfer() {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.batchTransfer(request, USER_ID, ORG_ID);
        }
    }

    private Customer customer(String id, String owner) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("customer-" + id);
        customer.setOwner(owner);
        return customer;
    }
}
