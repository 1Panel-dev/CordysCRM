package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerRelation;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAssociationPermissionServiceTest {
    private final CustomerAssociationPermissionService service = new CustomerAssociationPermissionService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> collaborations = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerRelation> relations = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    private final ResourcePermissionService permissions = mock(ResourcePermissionService.class);
    private final DataScopeService dataScope = mock(DataScopeService.class);
    private MockedStatic<SessionUtils> session;
    private MockedStatic<OrganizationContext> organization;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "collaborationMapper", collaborations);
        ReflectionTestUtils.setField(service, "relationMapper", relations);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        ReflectionTestUtils.setField(service, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        session = mockStatic(SessionUtils.class);
        organization = mockStatic(OrganizationContext.class);
        session.when(SessionUtils::getUserId).thenReturn("sales");
        organization.when(OrganizationContext::getOrganizationId).thenReturn("org");
    }

    @AfterEach
    void tearDown() {
        session.close();
        organization.close();
    }

    @Test
    void collaborationDeleteChecksPersistedCustomerId() {
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setId("collaboration");
        collaboration.setCustomerId("customer");
        when(collaborations.selectByPrimaryKey("collaboration")).thenReturn(collaboration);
        CustomerCollaborationService mutations = mutations();
        mutations.delete("collaboration");
        var order = inOrder(permissions, collaborations);
        order.verify(permissions).checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                "customer", FormKeyConstants.CUSTOMER, "sales", "org");
        order.verify(collaborations).deleteByPrimaryKey("collaboration");
    }

    @Test
    void batchChecksEveryRecordBeforeDeletingAnything() {
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setCustomerId("customer");
        when(collaborations.selectByPrimaryKey("valid")).thenReturn(collaboration);
        assertThrows(GenericException.class, () -> mutations().batchDelete(List.of("valid", "missing")));
        verify(collaborations, never()).deleteByIds(anyList());
    }

    @Test
    void deniedParentStopsCollaborationDeletion() {
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setCustomerId("other-customer");
        when(collaborations.selectByPrimaryKey("collaboration")).thenReturn(collaboration);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkResourcePermission(anyString(), anyString(), anyString(), anyString(), anyString());
        assertThrows(GenericException.class, () -> mutations().delete("collaboration"));
        verify(collaborations, never()).deleteByPrimaryKey(any());
    }

    private CustomerCollaborationService mutations() {
        CustomerCollaborationService mutations = new CustomerCollaborationService();
        ReflectionTestUtils.setField(mutations, "associationPermissionService", service);
        ReflectionTestUtils.setField(mutations, "customerCollaborationMapper", collaborations);
        return mutations;
    }

    @Test
    void relationCanBeMaintainedFromEitherAuthorizedEndButNotAcrossOrganizations() {
        CustomerRelation relation = new CustomerRelation();
        relation.setSourceCustomerId("source");
        relation.setTargetCustomerId("target");
        when(relations.selectByPrimaryKey("relation")).thenReturn(relation);
        Customer source = customer("source", "other-sales");
        Customer target = customer("target", "sales");
        when(customers.selectByPrimaryKey("source")).thenReturn(source);
        when(customers.selectByPrimaryKey("target")).thenReturn(target);
        when(dataScope.hasDataPermission("sales", "org", "sales",
                PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)).thenReturn(true);
        assertDoesNotThrow(() -> service.checkRelation("relation"));
        target.setOwner("other-sales");
        assertThrows(GenericException.class, () -> service.checkRelation("relation"));
        source.setOwner("sales");
        assertDoesNotThrow(() -> service.checkRelation("relation"));
        target.setOrganizationId("other-org");
        assertThrows(GenericException.class, () -> service.checkRelation("relation"));
        assertThrows(GenericException.class, () -> service.checkRelation("missing"));
    }

    @Test
    void customerProviderDoesNotExposeCrossOrganizationResources() {
        CustomerResourceAccessContextProvider provider = new CustomerResourceAccessContextProvider();
        ReflectionTestUtils.setField(provider, "customerMapper", customers);
        Customer customer = customer("customer", "sales");
        when(customers.selectByPrimaryKey("customer")).thenReturn(customer);
        when(customers.selectByIds(List.of("customer"))).thenReturn(List.of(customer));
        assertNotNull(provider.getAccessContext("customer", "org"));
        assertEquals("sales", provider.batchGetOwnerIds(List.of("customer"), "org").get("customer"));
        assertNull(provider.getAccessContext("customer", "other-org"));
        assertTrue(provider.batchGetOwnerIds(List.of("customer"), "other-org").isEmpty());
    }

    private Customer customer(String id, String owner) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setOwner(owner);
        customer.setOrganizationId("org");
        return customer;
    }
}
