package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FollowUpPlanJointFollowPermissionTest {
    private static final String USER = "joint-member";
    private static final String ORG = "org-a";
    private static final String CUSTOMER = "customer-a";
    private final FollowUpPlanService service = new FollowUpPlanService();
    private final CustomerCollaborationService collaborations = new CustomerCollaborationService();
    private final DataScopeService dataScope = mock(DataScopeService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> members = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Opportunity> opportunities = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<FollowUpPlan> plans = mock(BaseMapper.class);
    private final FollowUpPlanFieldService fields = mock(FollowUpPlanFieldService.class);
    private final StatisticFieldService statistics = mock(StatisticFieldService.class);
    private final BaseService base = mock(BaseService.class);
    private final Customer customer = new Customer();
    private final CustomerCollaboration member = new CustomerCollaboration();
    private final FollowUpPlan plan = new FollowUpPlan();
    private MockedStatic<IDGenerator> ids;
    private MockedStatic<Translator> translator;

    @BeforeEach
    void configureRealJointFollowScope() {
        ids = mockStatic(IDGenerator.class);
        ids.when(IDGenerator::nextStr).thenReturn("new-plan");
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        customer.setId(CUSTOMER);
        customer.setOrganizationId(ORG);
        customer.setOwner("customer-owner");
        customer.setInSharedPool(false);
        member.setCustomerId(CUSTOMER);
        member.setUserId(USER);
        member.setCollaborationType("JOINT_FOLLOW");
        plan.setId("plan-a");
        plan.setOrganizationId(ORG);
        plan.setCustomerId(CUSTOMER);
        plan.setType("CUSTOMER");
        plan.setOwner(USER);
        plan.setStatus("PREPARED");
        plan.setConverted(false);
        when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(customer);
        when(members.select(any(CustomerCollaboration.class))).thenAnswer(invocation -> {
            CustomerCollaboration query = invocation.getArgument(0);
            return CUSTOMER.equals(query.getCustomerId())
                    && (query.getUserId() == null || USER.equals(query.getUserId())) ? List.of(member) : List.of();
        });
        when(plans.selectByPrimaryKey("plan-a")).thenReturn(plan);
        ReflectionTestUtils.setField(collaborations, "customerCollaborationMapper", members);
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, BaseFollowUpService.class, "customerMapper", customers, null);
        ReflectionTestUtils.setField(service, "opportunityMapper", opportunities);
        ReflectionTestUtils.setField(service, "followUpPlanMapper", plans);
        ReflectionTestUtils.setField(service, "followUpPlanFieldService", fields);
        ReflectionTestUtils.setField(service, "statisticFieldService", statistics);
        ReflectionTestUtils.setField(service, "baseService", base);
    }

    @AfterEach
    void closeIds() {
        ids.close();
        translator.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "READ_ONLY", "JOINT_FOLLOW" })
    void canAssignPlanToAnotherUserWithinCustomerRoleScope(String collaboration) {
        member.setCollaborationType(collaboration);
        when(dataScope.hasDataPermission(USER, ORG, customer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId(CUSTOMER);
        request.setType("CUSTOMER");
        request.setOwner("other-user");

        FollowUpPlan result = service.add(request, USER, ORG);

        assertEquals("other-user", result.getOwner());
        verify(plans).insert(result);
    }

    @Test
    void canAssignOpportunityPlanToAnotherUserWhenItsCustomerIsReadOnly() {
        member.setCollaborationType("READ_ONLY");
        Opportunity opportunity = new Opportunity();
        opportunity.setOrganizationId(ORG);
        opportunity.setCustomerId(CUSTOMER);
        opportunity.setOwner("opportunity-owner");
        when(opportunities.selectByPrimaryKey("opportunity-a")).thenReturn(opportunity);
        when(dataScope.hasDataPermission(USER, ORG, opportunity.getOwner(),
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)).thenReturn(true);
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setType("CUSTOMER");
        request.setOpportunityId("opportunity-a");
        request.setCustomerId(CUSTOMER);
        request.setOwner("other-user");

        FollowUpPlan result = service.add(request, USER, ORG);

        assertEquals("other-user", result.getOwner());
        verify(plans).insert(result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "CUSTOMER", "customer" })
    void jointFollowAloneCannotAssignAnotherOwnersPlan(String type) {
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId(CUSTOMER);
        request.setType(type);
        request.setOwner("other-user");

        GenericException exception = assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(plans, fields, statistics);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MISSING", "OTHER_ORG", "POOL" })
    void roleScopeCannotAuthorizeInvalidCustomer(String scenario) {
        when(dataScope.hasDataPermission(USER, ORG, customer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_READ))
                .thenReturn(true);
        switch (scenario) {
        case "MISSING" -> when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(null);
        case "OTHER_ORG" -> customer.setOrganizationId("other-org");
        case "POOL" -> customer.setInSharedPool(true);
        default -> throw new AssertionError(scenario);
        }
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId(CUSTOMER);
        request.setType("CUSTOMER");

        GenericException exception = assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(plans, fields, statistics);
    }

    @ParameterizedTest
    @ValueSource(strings = { "OWNER", "JOINT_FOLLOW" })
    void creatingPlanWithoutOwnerDefaultsToCurrentUserBeforeCheckingScope(String access) {
        if ("OWNER".equals(access)) {
            customer.setOwner(USER);
        }
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId(CUSTOMER);
        request.setType("CUSTOMER");

        FollowUpPlan result = service.add(request, USER, ORG);

        assertEquals(USER, result.getOwner());
        verify(plans).insert(result);
    }

    @ParameterizedTest
    @CsvSource({ "READ_ONLY,true", "READ_ONLY,false", "NO_RELATION,true" })
    void creatingPlanRequiresWriteAccessToAssociatedCustomer(String collaboration, boolean direct) {
        if ("NO_RELATION".equals(collaboration)) {
            when(members.select(any(CustomerCollaboration.class))).thenReturn(List.of());
        } else {
            member.setCollaborationType(collaboration);
        }
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setType("CUSTOMER");
        if (direct) {
            request.setCustomerId(CUSTOMER);
        } else {
            Opportunity opportunity = new Opportunity();
            opportunity.setOrganizationId(ORG);
            opportunity.setCustomerId(CUSTOMER);
            opportunity.setOwner("opportunity-owner");
            when(opportunities.selectByPrimaryKey("opportunity-a")).thenReturn(opportunity);
            request.setOpportunityId("opportunity-a");
        }

        GenericException exception = assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(plans, fields, statistics);
    }

    @Test
    void jointFollowCanEditAnotherOwnersPlanAndChangeItsStatus() {
        plan.setOwner("other-user");
        FollowUpPlanUpdateRequest update = new FollowUpPlanUpdateRequest();
        update.setId("plan-a");
        update.setCustomerId(CUSTOMER);
        update.setType("CUSTOMER");
        update.setOwner("new-owner");
        FollowUpPlanStatusRequest status = new FollowUpPlanStatusRequest();
        status.setId("plan-a");
        status.setStatus("UNDERWAY");

        service.update(update, USER, ORG);
        service.updateStatus(status, USER, ORG);

        verify(plans, times(2)).update(any(FollowUpPlan.class));
    }

    @Test
    void cannotEditAnotherOrganizationsPlanOrChangeItsStatus() {
        plan.setOrganizationId("other-org");
        FollowUpPlanUpdateRequest update = new FollowUpPlanUpdateRequest();
        update.setId(plan.getId());
        FollowUpPlanStatusRequest status = new FollowUpPlanStatusRequest();
        status.setId(plan.getId());
        status.setStatus("UNDERWAY");

        assertEquals(CrmHttpResultCode.FORBIDDEN,
                assertThrows(GenericException.class, () -> service.update(update, USER, ORG)).getErrorCode());
        assertEquals(CrmHttpResultCode.FORBIDDEN,
                assertThrows(GenericException.class, () -> service.updateStatus(status, USER, ORG)).getErrorCode());

        verify(plans, never()).update(any());
        verifyNoInteractions(fields, statistics, base);
    }
}
