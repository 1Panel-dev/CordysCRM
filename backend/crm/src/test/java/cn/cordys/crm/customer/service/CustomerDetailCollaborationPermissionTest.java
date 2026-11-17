package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CustomerDetailCollaborationPermissionTest {
    private final CustomerService service = spy(new CustomerService());
    private final CustomerCollaborationService collaborations = new CustomerCollaborationService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> collaborationMapper = mock(BaseMapper.class);
    private final DataScopeService dataScope = mock(DataScopeService.class);
    private final ResourcePermissionService permissions = mock(ResourcePermissionService.class);
    private final RelatedResourcePermissionService relatedPermissions = mock(RelatedResourcePermissionService.class);
    private final CustomerGetResponse customer = new CustomerGetResponse();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(service, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(collaborations, "customerCollaborationMapper", collaborationMapper);
        customer.setId("customer");
        customer.setOwner("owner");
        doReturn(customer).when(service).get("customer");
        collaboration("COLLABORATION", null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "role", "related", "approval", "collaboration" })
    void everyReadPathReturnsTheEffectiveCollaborationRestriction(String readSource) {
        when(dataScope.hasDataPermission("member", "org", "owner", PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn("role".equals(readSource));
        when(relatedPermissions.hasRelatedReadPermission(FormKey.CUSTOMER.getKey(), "customer", "member", "org"))
                .thenReturn("related".equals(readSource));
        when(permissions.hasApprovalViewPermission("customer", "member", "org", FormKey.CUSTOMER.getKey()))
                .thenReturn("approval".equals(readSource));

        assertEquals("COLLABORATION", service.getWithDataPermissionCheck("customer", "member", "org")
                .getCollaborationType());
    }

    @ParameterizedTest
    @CsvSource({ "READ_ONLY,,READ_ONLY", "JOINT_FOLLOW,,JOINT_FOLLOW",
            "READ_ONLY,1,COLLABORATION", "JOINT_FOLLOW,1,COLLABORATION" })
    void detailUsesTheExistingExpirationDowngrade(String type, Long expireTime, String expected) {
        collaboration(type, expireTime);
        assertEquals(expected, service.getWithDataPermissionCheck("customer", "member", "org")
                .getCollaborationType());
    }

    @Test
    void independentCustomerUpdatePermissionIsNotDowngradedByCollaboration() {
        when(dataScope.hasDataPermission("member", "org", "owner", PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE))
                .thenReturn(true);
        assertNull(service.getWithDataPermissionCheck("customer", "member", "org").getCollaborationType());
    }

    @Test
    void customerOwnerIsNotDowngradedByCollaboration() {
        customer.setOwner("member");
        assertNull(service.getWithDataPermissionCheck("customer", "member", "org").getCollaborationType());
    }

    @Test
    void unrelatedUserStillRequiresAReadPermission() {
        when(collaborationMapper.select(any(CustomerCollaboration.class))).thenReturn(List.of());
        assertThrows(GenericException.class, () -> service.getWithDataPermissionCheck("customer", "member", "org"));
        when(dataScope.hasDataPermission("member", "org", "owner", PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
        assertNull(service.getWithDataPermissionCheck("customer", "member", "org").getCollaborationType());
    }

    private void collaboration(String type, Long expireTime) {
        when(collaborationMapper.select(any(CustomerCollaboration.class))).thenAnswer(invocation -> {
            CustomerCollaboration member = new CustomerCollaboration();
            member.setCustomerId("customer");
            member.setUserId("member");
            member.setCollaborationType(type);
            member.setExpireTime(expireTime);
            return List.of(member);
        });
    }
}
