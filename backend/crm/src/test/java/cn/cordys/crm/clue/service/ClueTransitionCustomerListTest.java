package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.response.ClueTransitionCustomerListResponse;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ClueTransitionCustomerListTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String OWNER_ID = "customer-owner";
    private static final String CLUE_ID = "converted-clue";
    private static final String CUSTOMER_ID = "converted-customer";

    private final ClueService service = new ClueService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
    private final CustomerService customerService = mock(CustomerService.class);
    private final BaseService baseService = mock(BaseService.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final CustomerCollaborationService collaborationService = mock(CustomerCollaborationService.class);
    private final PoolCustomerService poolCustomerService = mock(PoolCustomerService.class);
    private final Clue clue = new Clue();
    private final Customer customer = new Customer();
    private MockedStatic<PermissionUtils> permissions;

    @BeforeEach
    void setUp() {
        // 全局 subclass 引擎不支持静态方法，此测试显式使用 inline 引擎。
        permissions = mockStatic(PermissionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborationService);
        ReflectionTestUtils.setField(service, "poolCustomerService", poolCustomerService);

        clue.setId(CLUE_ID);
        clue.setOrganizationId(ORG_ID);
        clue.setTransitionType("CUSTOMER");
        clue.setTransitionId(CUSTOMER_ID);
        when(clueMapper.selectByPrimaryKey(CLUE_ID)).thenReturn(clue);
        customer.setId(CUSTOMER_ID);
        customer.setOrganizationId(ORG_ID);
        customer.setName("示例客户");
        customer.setOwner(OWNER_ID);
        customer.setInSharedPool(false);
        customer.setCreateTime(1_000L);
        customer.setFollowTime(2_000L);
        customer.setCreateUser("creator");
        customer.setUpdateUser("updater");
        customer.setUpdateTime(3_000L);
        customer.setCollectionTime(4_000L);
        customer.setFollower("follower");
        customer.setReasonId("recycle-reason");
        customer.setFrozen(true);
        customer.setFreezeReason("冻结原因");
        customer.setApprovalStatus("APPROVING");
        customer.setApproved(true);
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customer);
        when(baseService.getUserName(OWNER_ID)).thenReturn("客户负责人");
        when(baseService.getUserDeptMapByUserId(OWNER_ID, ORG_ID))
                .thenReturn(new UserDeptDTO(OWNER_ID, "department", "示例部门"));
        when(customerService.buildListData(anyList(), eq(ORG_ID))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerService.buildOptionMap(eq(ORG_ID), anyList(), anyList())).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        permissions.close();
    }

    @Test
    void missingClueCrossOrganizationAndBlankOrganizationAreRejected() {
        when(clueMapper.selectByPrimaryKey(CLUE_ID)).thenReturn(null);
        assertThrows(GenericException.class, this::query);

        when(clueMapper.selectByPrimaryKey(CLUE_ID)).thenReturn(clue);
        clue.setOrganizationId("other-org");
        assertThrows(GenericException.class, this::query);

        clue.setOrganizationId(ORG_ID);
        assertThrows(GenericException.class, () -> service.getTransitionCustomerList(CLUE_ID, USER_ID, null));
        assertThrows(GenericException.class, () -> service.getTransitionCustomerList(CLUE_ID, USER_ID, " "));
        verifyNoInteractions(customerMapper, customerService);
    }

    @Test
    void absentOrNonCustomerTransitionReturnsEmptyList() {
        clue.setTransitionType(null);
        assertEmpty(query());
        clue.setTransitionType("CLUE");
        assertEmpty(query());
        clue.setTransitionType("CUSTOMER");
        clue.setTransitionId(" ");
        assertEmpty(query());
        verifyNoInteractions(customerMapper, customerService);
    }

    @Test
    void missingOrCrossOrganizationCustomerReturnsEmptyList() {
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(null);
        assertEmpty(query());
        when(customerMapper.selectByPrimaryKey(CUSTOMER_ID)).thenReturn(customer);
        customer.setOrganizationId("other-org");
        assertEmpty(query());
        verifyNoInteractions(customerService, dataScopeService, collaborationService, poolCustomerService);
    }

    @Test
    void privateCustomerOwnerUsesCompleteCustomerListAssembly() {
        allowPrivateRead();
        customer.setOwner(USER_ID);
        CustomerListResponse built = new CustomerListResponse();
        built.setId(CUSTOMER_ID);
        built.setModuleFields(List.of());
        List<CustomerListResponse> builtList = List.of(built);
        Map<String, List<OptionDTO>> options = Map.of("owner", List.of(new OptionDTO(USER_ID, "客户负责人")));
        when(customerService.buildListData(anyList(), eq(ORG_ID))).thenReturn(builtList);
        when(customerService.buildOptionMap(eq(ORG_ID), anyList(), same(builtList))).thenReturn(options);

        ClueTransitionCustomerListResponse response = query();

        assertTrue(response.isHasPermission());
        assertSame(builtList, response.getList());
        assertSame(options, response.getOptionMap());
        ArgumentCaptor<List<CustomerListResponse>> source = ArgumentCaptor.forClass(List.class);
        verify(customerService).buildListData(source.capture(), eq(ORG_ID));
        CustomerListResponse raw = source.getValue().getFirst();
        assertEquals(CUSTOMER_ID, raw.getId());
        assertEquals("creator", raw.getCreateUser());
        assertEquals("APPROVING", raw.getApprovalStatus());
        assertTrue(raw.getFrozen());
        verify(customerService).buildOptionMap(ORG_ID, builtList, builtList);
    }

    @ParameterizedTest
    @ValueSource(strings = { "READ_ONLY", "COLLABORATION" })
    void privateCollaboratorCanReadOutsideOwnerDataScope(String collaborationType) {
        allowPrivateRead();
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setCollaborationType(collaborationType);
        when(collaborationService.selectByCustomerIdAndUserId(CUSTOMER_ID, USER_ID)).thenReturn(List.of(collaboration));

        ClueTransitionCustomerListResponse response = query();

        assertTrue(response.isHasPermission());
        assertEquals(collaborationType, response.getList().getFirst().getCollaborationType());
        verify(customerService).buildListData(anyList(), eq(ORG_ID));
    }

    @Test
    void privateCustomerWithoutReadPermissionOnlyReturnsBasicFields() {
        when(dataScopeService.hasDataPermission(USER_ID, ORG_ID, OWNER_ID,
                PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setCollaborationType("COLLABORATION");
        when(collaborationService.selectByCustomerIdAndUserId(CUSTOMER_ID, USER_ID)).thenReturn(List.of(collaboration));

        assertBasicFieldsOnly(query());
    }

    @Test
    void privateCustomerDataScopePermissionWithoutOwnershipOrCollaborationOnlyReturnsBasicFields() {
        allowPrivateRead();
        when(dataScopeService.hasDataPermission(USER_ID, ORG_ID, OWNER_ID,
                PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);

        assertBasicFieldsOnly(query());
        verifyNoInteractions(dataScopeService);
    }

    @Test
    void privateCustomerOwnerWithoutReadPermissionOnlyReturnsBasicFields() {
        customer.setOwner(USER_ID);
        when(baseService.getUserName(USER_ID)).thenReturn("客户负责人");
        when(baseService.getUserDeptMapByUserId(USER_ID, ORG_ID))
                .thenReturn(new UserDeptDTO(USER_ID, "department", "示例部门"));

        assertBasicFieldsOnly(query());
    }

    @Test
    void privateCustomerWithBlankOwnerDoesNotCheckOwnerDataScope() {
        allowPrivateRead();
        customer.setOwner(" ");

        assertFalse(query().isHasPermission());
        verifyNoInteractions(dataScopeService, customerService);
    }

    @Test
    void publicPoolMemberWithPoolReadPermissionCanReadCompleteCustomer() {
        customer.setInSharedPool(true);
        customer.setPoolId("customer-pool");
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ))
                .thenReturn(true);
        when(poolCustomerService.hasPoolMember("customer-pool", USER_ID, ORG_ID)).thenReturn(true);

        assertTrue(query().isHasPermission());
        verify(customerService).buildListData(anyList(), eq(ORG_ID));
        verifyNoInteractions(dataScopeService, collaborationService);
    }

    @Test
    void publicPoolCustomerRequiresBothPoolReadPermissionAndMembership() {
        customer.setInSharedPool(true);
        customer.setPoolId("customer-pool");
        allowPrivateRead();
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ))
                .thenReturn(true);
        when(dataScopeService.hasDataPermission(USER_ID, ORG_ID, OWNER_ID,
                PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
        assertBasicFieldsOnly(query());

        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ))
                .thenReturn(false);
        when(poolCustomerService.hasPoolMember("customer-pool", USER_ID, ORG_ID)).thenReturn(true);
        assertBasicFieldsOnly(query());
        verifyNoInteractions(dataScopeService, collaborationService);
    }

    @Test
    void frozenStateMatchesPermanentFutureExpiredAndNotFrozenCustomers() {
        allowPrivateRead();
        customer.setOwner(USER_ID);
        assertTrue(query().getList().getFirst().getFrozen());
        customer.setUnfreezeTime(Long.MAX_VALUE);
        assertTrue(query().getList().getFirst().getFrozen());
        customer.setUnfreezeTime(1L);
        assertFalse(query().getList().getFirst().getFrozen());
        customer.setFrozen(false);
        customer.setUnfreezeTime(Long.MAX_VALUE);
        assertFalse(query().getList().getFirst().getFrozen());
    }

    private ClueTransitionCustomerListResponse query() {
        return service.getTransitionCustomerList(CLUE_ID, USER_ID, ORG_ID);
    }

    private void allowPrivateRead() {
        permissions.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
    }

    private void assertEmpty(ClueTransitionCustomerListResponse response) {
        assertFalse(response.isHasPermission());
        assertEquals(List.of(), response.getList());
        assertEquals(Map.of(), response.getOptionMap());
    }

    private void assertBasicFieldsOnly(ClueTransitionCustomerListResponse response) {
        assertFalse(response.isHasPermission());
        assertEquals(Map.of(), response.getOptionMap());
        assertEquals(1, response.getList().size());
        CustomerListResponse item = response.getList().getFirst();
        assertEquals(CUSTOMER_ID, item.getId());
        assertEquals("示例客户", item.getName());
        assertEquals(customer.getOwner(), item.getOwner());
        assertEquals("客户负责人", item.getOwnerName());
        assertEquals("department", item.getDepartmentId());
        assertEquals("示例部门", item.getDepartmentName());
        assertEquals(2_000L, item.getFollowTime());
        assertEquals(1_000L, item.getCreateTime());
        // 无客户数据权限时只能暴露默认表头，不能夹带客户列表的扩展信息。
        assertNull(item.getModuleFields());
        assertNull(item.getFrozen());
        assertNull(item.getFreezeReason());
        assertNull(item.getApprovalStatus());
        assertNull(item.getApproved());
        assertNull(item.getCreateUser());
        assertNull(item.getUpdateUser());
        assertNull(item.getUpdateTime());
        assertNull(item.getReasonId());
        assertNull(item.getCollectionTime());
        assertNull(item.getRecyclePoolName());
        assertNull(item.getCollaborationType());
        verifyNoInteractions(customerService);
    }
}
