package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class FollowUpRecordAddPermissionTest {
    private static final String USER = "user-a";
    private static final String ORG = "org-a";
    private static final String CUSTOMER = "customer-a";
    private static final String CLUE = "clue-a";
    private static final String OPPORTUNITY = "opportunity-a";

    private final FollowUpRecordService service = new FollowUpRecordService();
    private final DataScopeService dataScope = mock(DataScopeService.class);
    private final CustomerCollaborationService collaborations = mock(CustomerCollaborationService.class);
    private final BaseMapper<FollowUpRecord> records = mock(BaseMapper.class);
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    private final BaseMapper<Clue> clues = mock(BaseMapper.class);
    private final BaseMapper<Opportunity> opportunities = mock(BaseMapper.class);
    private final FollowUpRecordFieldService fields = mock(FollowUpRecordFieldService.class);
    private final StatisticFieldService statistics = mock(StatisticFieldService.class);
    private final CommonNoticeSendService notices = mock(CommonNoticeSendService.class);

    @BeforeEach
    void configureDependencies() {
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(service, BaseFollowUpService.class, "customerMapper", customers, null);
        ReflectionTestUtils.setField(service, "followUpRecordMapper", records);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        ReflectionTestUtils.setField(service, "clueMapper", clues);
        ReflectionTestUtils.setField(service, "opportunityMapper", opportunities);
        ReflectionTestUtils.setField(service, "followUpRecordFieldService", fields);
        ReflectionTestUtils.setField(service, "statisticFieldService", statistics);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", notices);
        ReflectionTestUtils.setField(service, "baseService", mock(BaseService.class));
    }

    @ParameterizedTest
    @CsvSource({ "other-user,org-a,false", "user-a,other-org,false", "user-a,org-a,true" })
    void rejectsCustomerWithoutWriteAccess(String owner, String orgId, boolean sharedPool) {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOwner(USER);
        stubCustomer(owner, orgId, sharedPool);

        assertForbiddenBeforeWrites(request);
    }

    @Test
    void rejectsReadOnlyCollaboratorForAnotherUsersCustomer() {
        stubCustomer("other-user", ORG, false);
        stubCollaboration(CustomerCollaborationType.READ_ONLY);

        assertForbiddenBeforeWrites(customerRequest());
    }

    @Test
    void rejectsClueOutsideRoleScopeEvenWhenCustomerIsOwnedByCurrentUser() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setClueId(CLUE);
        stubCustomer(USER, ORG, false);
        stubClue("other-user");

        assertForbiddenBeforeWrites(request);
    }

    @Test
    void rejectsOpportunityOutsideRoleScopeEvenWhenCustomerIsOwnedByCurrentUser() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOpportunityId(OPPORTUNITY);
        stubCustomer(USER, ORG, false);
        stubOpportunity("other-user", CUSTOMER);

        assertForbiddenBeforeWrites(request);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", CUSTOMER })
    void addsRecordForOwnedOpportunityWithReadOnlyCustomer(String customerId) {
        FollowUpRecordAddRequest request = customerRequest();
        request.setCustomerId(customerId);
        request.setOpportunityId(OPPORTUNITY);
        stubOpportunity(USER, CUSTOMER);
        stubCustomer("other-user", ORG, false);
        stubCollaboration(CustomerCollaborationType.READ_ONLY);

        assertRecordAdded(request);
        verifyNoInteractions(collaborations);
    }

    @Test
    void rejectsUnrelatedCustomerForOwnedOpportunity() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOpportunityId(OPPORTUNITY);
        stubOpportunity(USER, "other-customer");
        stubCustomer(USER, ORG, false);

        assertForbiddenBeforeWrites(request);
    }

    @Test
    void rejectsCrossOrganizationCustomerForOwnedOpportunity() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOpportunityId(OPPORTUNITY);
        stubOpportunity(USER, CUSTOMER);
        stubCustomer(USER, "other-org", false);

        assertForbiddenBeforeWrites(request);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", CUSTOMER })
    void rejectsMissingRelatedResource(String customerId) {
        FollowUpRecordAddRequest request = customerRequest();
        request.setCustomerId(customerId);

        assertForbiddenBeforeWrites(request);
    }

    @ParameterizedTest
    @ValueSource(strings = { "COLLABORATION", "JOINT_FOLLOW" })
    void addsRecordForWritableCollaboratingCustomer(String collaborationType) {
        stubCustomer("other-user", ORG, false);
        stubCollaboration(CustomerCollaborationType.valueOf(collaborationType));

        assertRecordAdded(customerRequest());
    }

    @Test
    void readOnlyCollaborationDoesNotOverrideCustomerRoleScopeWhenAssigningAnotherOwner() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOwner("assigned-user");
        stubCustomer("other-user", ORG, false);
        stubCollaboration(CustomerCollaborationType.READ_ONLY);
        when(dataScope.hasDataPermission(USER, ORG, "other-user", PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);

        assertRecordAdded(request);
    }

    @Test
    void canAddForClueInRoleScope() {
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setType("CLUE");
        request.setClueId(CLUE);
        stubClue("other-user");
        when(dataScope.hasDataPermission(USER, ORG, "other-user", PermissionConstants.CLUE_MANAGEMENT_READ))
                .thenReturn(true);

        assertRecordAdded(request);
    }

    @Test
    void canAddForOpportunityInRoleScope() {
        FollowUpRecordAddRequest request = customerRequest();
        request.setOpportunityId(OPPORTUNITY);
        stubOpportunity("other-user", CUSTOMER);
        stubCustomer("another-customer-owner", ORG, false);
        when(dataScope.hasDataPermission(USER, ORG, "other-user", PermissionConstants.OPPORTUNITY_MANAGEMENT_READ))
                .thenReturn(true);

        assertRecordAdded(request);
    }

    @ParameterizedTest
    @ValueSource(strings = { "CUSTOMER", "CLUE" })
    void addsRecordForMyRelatedResources(String type) {
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setType(type);
        if ("CUSTOMER".equals(type)) {
            request.setCustomerId(CUSTOMER);
            request.setOpportunityId(OPPORTUNITY);
            stubCustomer(USER, ORG, false);
            stubOpportunity(USER, CUSTOMER);
        } else {
            request.setClueId(CLUE);
            stubClue(USER);
        }

        assertRecordAdded(request);
    }

    private void assertRecordAdded(FollowUpRecordAddRequest request) {
        try (var ids = mockStatic(IDGenerator.class)) {
            ids.when(IDGenerator::nextStr).thenReturn("record-a");
            FollowUpRecord result = service.add(request, USER, ORG);

            assertEquals(request.getOwner() == null ? USER : request.getOwner(), result.getOwner());
            verify(records).insert(result);
        }
    }

    private FollowUpRecordAddRequest customerRequest() {
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setType("CUSTOMER");
        request.setCustomerId(CUSTOMER);
        return request;
    }

    private void stubCustomer(String owner, String orgId, boolean sharedPool) {
        Customer customer = new Customer();
        customer.setOrganizationId(orgId);
        customer.setOwner(owner);
        customer.setInSharedPool(sharedPool);
        when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(customer);
    }

    private void stubClue(String owner) {
        Clue clue = new Clue();
        clue.setOrganizationId(ORG);
        clue.setOwner(owner);
        when(clues.selectByPrimaryKey(CLUE)).thenReturn(clue);
    }

    private void stubCollaboration(CustomerCollaborationType type) {
        CustomerCollaboration member = new CustomerCollaboration();
        member.setUserId(USER);
        member.setCollaborationType(type.name());
        when(collaborations.selectByCustomerIdAndUserId(CUSTOMER, USER)).thenReturn(List.of(member));
    }

    private void stubOpportunity(String owner, String customerId) {
        Opportunity opportunity = new Opportunity();
        opportunity.setOrganizationId(ORG);
        opportunity.setOwner(owner);
        opportunity.setCustomerId(customerId);
        when(opportunities.selectByPrimaryKey(OPPORTUNITY)).thenReturn(opportunity);
    }

    private void assertForbiddenBeforeWrites(FollowUpRecordAddRequest request) {
        GenericException exception = assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(records, fields, statistics, notices);
        verify(customers, never()).update(any());
        verify(clues, never()).update(any());
        verify(opportunities, never()).update(any());
    }
}
