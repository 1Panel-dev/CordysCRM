package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CustomerJointFollowOperationPermissionTest {
    private static final String USER = "joint-member";
    private static final String ORG = "org-a";
    private static final String CUSTOMER = "customer-a";
    private final ResourcePermissionService service = new ResourcePermissionService();
    private final CustomerResourceAccessContextProvider provider = new CustomerResourceAccessContextProvider();
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final ApprovalFlowService approvals = mock(ApprovalFlowService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> members = mock(BaseMapper.class);
    private final Map<String, Customer> customerData = new HashMap<>();
    private final List<CustomerCollaboration> membership = new ArrayList<>();
    private MockedStatic<PermissionUtils> roles;

    @BeforeEach
    void configureRealCustomerPermissionChain() {
        roles = mockStatic(PermissionUtils.class);
        roles.when(() -> PermissionUtils.hasPermission(anyString())).thenReturn(true);
        customerData.put(CUSTOMER, customer(CUSTOMER, "other-owner"));
        membership.add(member(CUSTOMER));
        when(customers.selectByPrimaryKey(anyString()))
                .thenAnswer(invocation -> customerData.get(invocation.<String>getArgument(0)));
        when(customers.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.stream().map(customerData::get).filter(Objects::nonNull).toList();
        });
        when(members.select(any(CustomerCollaboration.class)))
                .thenAnswer(invocation -> matchingMembers(invocation.getArgument(0)));
        when(members.countByExample(any(CustomerCollaboration.class)))
                .thenAnswer(invocation -> (long) matchingMembers(invocation.getArgument(0)).size());
        DeptDataPermissionDTO self = new DeptDataPermissionDTO();
        self.setSelf(true);
        doReturn(self).when(dataScope).getDeptDataPermission(anyString(), anyString(), anyString());
        ReflectionTestUtils.setField(provider, "customerMapper", customers);
        ReflectionTestUtils.setField(provider, "collaborationMapper", members);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "relatedResourcePermissionService",
                mock(RelatedResourcePermissionService.class));
        ReflectionTestUtils.setField(service, "approvalFlowService", approvals);
        ReflectionTestUtils.setField(service, "contextProviders", List.of(provider));
        ReflectionTestUtils.invokeMethod(service, "initializeProviders");
    }

    @AfterEach
    void closeRoleContext() {
        roles.close();
    }

    static Stream<String> customerPermissions() {
        return Stream.concat(Stream.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ), writePermissions());
    }

    static Stream<String> writePermissions() {
        return Stream.of(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                PermissionConstants.CUSTOMER_MANAGEMENT_TRANSFER,
                PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE,
                PermissionConstants.CUSTOMER_MANAGEMENT_DELETE);
    }

    @ParameterizedTest
    @MethodSource("customerPermissions")
    void effectiveJointFollowAllowsSingleAndBatchOperationsOutsideOwnScope(String permission) {
        membership.getFirst().setExpireTime(Long.MAX_VALUE);
        assertDoesNotThrow(() -> single(permission));
        assertDoesNotThrow(() -> batch(permission, CUSTOMER));
    }

    @ParameterizedTest
    @MethodSource("customerPermissions")
    void collaborationCannotReplaceTheRequiredRolePermission(String permission) {
        roles.when(() -> PermissionUtils.hasPermission(permission)).thenReturn(false);

        assertThrows(GenericException.class, () -> single(permission));
        assertThrows(GenericException.class, () -> batch(permission, CUSTOMER));
    }

    static Stream<Arguments> invalidWriteCollaborations() {
        return writePermissions().flatMap(permission -> Stream.of("READ_ONLY", "COLLABORATION", "EXPIRED", "POOL")
                .map(type -> Arguments.of(permission, type)));
    }

    @ParameterizedTest
    @MethodSource("invalidWriteCollaborations")
    void onlyEffectiveJointFollowAddsWriteScope(String permission, String type) {
        if ("EXPIRED".equals(type)) {
            membership.getFirst().setExpireTime(1L);
        } else if ("POOL".equals(type)) {
            customerData.get(CUSTOMER).setInSharedPool(true);
        } else {
            membership.getFirst().setCollaborationType(type);
        }

        assertThrows(GenericException.class, () -> single(permission));
        assertThrows(GenericException.class, () -> batch(permission, CUSTOMER));
    }

    static Stream<Arguments> invalidCustomers() {
        return customerPermissions().flatMap(permission -> Stream.of("REMOVED", "OTHER_ORG", "MISSING", "NO_OWNER")
                .map(scenario -> Arguments.of(permission, scenario)));
    }

    @ParameterizedTest
    @MethodSource("invalidCustomers")
    void collaborationCannotAuthorizeRemovedMissingOrOtherOrganizationResources(String permission, String scenario) {
        switch (scenario) {
        case "REMOVED" -> membership.clear();
        case "OTHER_ORG" -> customerData.get(CUSTOMER).setOrganizationId("other-org");
        case "MISSING" -> customerData.clear();
        case "NO_OWNER" -> customerData.get(CUSTOMER).setOwner(null);
        default -> throw new AssertionError(scenario);
        }

        assertThrows(GenericException.class, () -> single(permission));
        assertThrows(GenericException.class, () -> batch(permission, CUSTOMER));
    }

    @ParameterizedTest
    @MethodSource("writePermissions")
    void oneCustomerCollaborationCannotAuthorizeAnotherCustomerWithTheSameOwner(String permission) {
        customerData.put("customer-b", customer("customer-b", "other-owner"));

        assertThrows(GenericException.class, () -> batch(permission, CUSTOMER, "customer-b"));

        membership.add(member("customer-b"));
        assertDoesNotThrow(() -> batch(permission, CUSTOMER, "customer-b"));
    }

    @ParameterizedTest
    @MethodSource("writePermissions")
    void mixedBatchCanCombineCollaborationAndOriginalOwnScope(String permission) {
        customerData.put("customer-b", customer("customer-b", USER));

        assertDoesNotThrow(() -> batch(permission, CUSTOMER, "customer-b"));
    }

    @ParameterizedTest
    @MethodSource("customerPermissions")
    void singleOperationStillHonorsApprovalStatusRestrictions(String permission) {
        customerData.get(CUSTOMER).setApprovalStatus("APPROVING");
        StatusPermissionDTO status = new StatusPermissionDTO();
        status.setApprovalStatus("APPROVING");
        status.setPermission(permission);
        status.setEnabled(false);
        StatusPermissionSettingResponse setting = new StatusPermissionSettingResponse();
        setting.setStatusPermissions(List.of(status));
        when(approvals.getStatusPermissionsByFormType(FormKeyConstants.CUSTOMER, ORG)).thenReturn(setting);

        assertThrows(GenericException.class, () -> single(permission));
    }

    @ParameterizedTest
    @ValueSource(strings = { PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT,
            PermissionConstants.CUSTOMER_MANAGEMENT_MERGE })
    void collaborationDoesNotAuthorizeExportOrMerge(String permission) {
        assertThrows(GenericException.class, () -> single(permission));
        assertThrows(GenericException.class, () -> batch(permission, CUSTOMER));
    }

    private void single(String permission) {
        service.checkResourcePermissionWithoutApprovalTask(permission, CUSTOMER, FormKeyConstants.CUSTOMER, USER, ORG);
    }

    private void batch(String permission, String... ids) {
        service.checkBatchResourcePermission(permission, List.of(ids), FormKeyConstants.CUSTOMER, USER, ORG);
    }

    private Customer customer(String id, String owner) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setOrganizationId(ORG);
        customer.setOwner(owner);
        customer.setApprovalStatus("NONE");
        return customer;
    }

    private CustomerCollaboration member(String customerId) {
        CustomerCollaboration member = new CustomerCollaboration();
        member.setCustomerId(customerId);
        member.setUserId(USER);
        member.setCollaborationType("JOINT_FOLLOW");
        return member;
    }

    private List<CustomerCollaboration> matchingMembers(CustomerCollaboration query) {
        return membership.stream().filter(member -> member.getCustomerId().equals(query.getCustomerId())
                && member.getUserId().equals(query.getUserId())).toList();
    }
}
