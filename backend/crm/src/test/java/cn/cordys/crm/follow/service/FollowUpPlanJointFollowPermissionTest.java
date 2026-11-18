package cn.cordys.crm.follow.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> members = mock(BaseMapper.class);
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
        ReflectionTestUtils.setField(service, BaseFollowUpService.class, "customerMapper", customers, null);
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
    @CsvSource({ "JOINT_FOLLOW,,true", "JOINT_FOLLOW,9223372036854775807,true", "JOINT_FOLLOW,1,false",
            "READ_ONLY,,false", "COLLABORATION,,false" })
    void onlyEffectiveJointFollowRestrictsPlanOwner(String type, Long expiry, boolean restricted) {
        member.setCollaborationType(type);
        member.setExpireTime(expiry);
        assertDoesNotThrow(() -> service.checkJointFollowPlanOwner(plan, USER, ORG, USER));
        plan.setOwner("other-user");
        Executable editOther = () -> service.checkJointFollowPlanOwner(plan, "other-user", ORG, USER);
        if (restricted) {
            assertThrows(GenericException.class, editOther);
        } else {
            assertDoesNotThrow(editOther, "未生效的共同跟进关系不额外限缩原角色范围");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "OWNER", "NO_RELATION", "POOL", "MISSING_CUSTOMER", "CLUE" })
    void ordinaryRoleScopeAndCustomerOwnerRemainUnchanged(String scenario) {
        switch (scenario) {
        case "OWNER" -> customer.setOwner(USER);
        case "NO_RELATION" -> when(members.select(any(CustomerCollaboration.class))).thenReturn(List.of());
        case "POOL" -> customer.setInSharedPool(true);
        case "MISSING_CUSTOMER" -> when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(null);
        case "CLUE" -> plan.setType("CLUE");
        default -> throw new AssertionError(scenario);
        }
        plan.setOwner("other-user");

        assertDoesNotThrow(() -> service.checkJointFollowPlanOwner(plan, "other-user", ORG, USER));
    }

    @Test
    void planFromAnotherOrganizationIsRejectedEvenWhenOwnedByCurrentUser() {
        plan.setOrganizationId("other-org");

        assertThrows(GenericException.class, () -> service.checkJointFollowPlanOwner(plan, USER, ORG, USER));
    }

    @Test
    void lowercaseCustomerTypeCannotBypassAnotherOwnersPlanRestriction() {
        plan.setType("customer");
        plan.setOwner("other-user");

        assertThrows(GenericException.class,
                () -> service.checkJointFollowPlanOwner(plan, "other-user", ORG, USER));
    }

    @Test
    void creatingPlanWithoutOwnerDefaultsToCurrentUserBeforeCheckingScope() {
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId(CUSTOMER);
        request.setType("CUSTOMER");

        FollowUpPlan result = service.add(request, USER, ORG);

        assertEquals(USER, result.getOwner());
        verify(plans).insert(result);
    }

    @Test
    void jointFollowCanEditOwnPlanAndChangeItsStatus() {
        FollowUpPlanUpdateRequest update = new FollowUpPlanUpdateRequest();
        update.setId("plan-a");
        update.setCustomerId(CUSTOMER);
        update.setType("CUSTOMER");
        update.setOwner(USER);
        FollowUpPlanStatusRequest status = new FollowUpPlanStatusRequest();
        status.setId("plan-a");
        status.setStatus("UNDERWAY");

        assertDoesNotThrow(() -> service.update(update, USER, ORG));
        assertDoesNotThrow(() -> service.updateStatus(status, USER, ORG));

        verify(plans, times(2)).update(any(FollowUpPlan.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADD", "UPDATE_OLD_OWNER", "UPDATE_NEW_OWNER", "UPDATE_DETACH_AND_TRANSFER",
            "UPDATE_INTO_JOINT_CUSTOMER", "STATUS" })
    void jointFollowCannotCreateOrChangeAnotherOwnersPlanBeforeAnyWrites(String operation) {
        FollowUpPlanUpdateRequest update = new FollowUpPlanUpdateRequest();
        update.setId("plan-a");
        update.setCustomerId(CUSTOMER);
        update.setType("CUSTOMER");
        update.setOwner("other-user");
        Executable write = switch (operation) {
        case "ADD" -> () -> service.add(update, USER, ORG);
        case "UPDATE_OLD_OWNER" -> {
            plan.setOwner("other-user");
            update.setOwner(USER);
            yield () -> service.update(update, USER, ORG);
        }
        case "UPDATE_NEW_OWNER" -> () -> service.update(update, USER, ORG);
        case "UPDATE_DETACH_AND_TRANSFER" -> {
            update.setCustomerId("unrelated-customer");
            update.setType("CLUE");
            yield () -> service.update(update, USER, ORG);
        }
        case "UPDATE_INTO_JOINT_CUSTOMER" -> {
            plan.setCustomerId("unrelated-customer");
            yield () -> service.update(update, USER, ORG);
        }
        case "STATUS" -> {
            plan.setOwner("other-user");
            FollowUpPlanStatusRequest status = new FollowUpPlanStatusRequest();
            status.setId("plan-a");
            status.setStatus("UNDERWAY");
            yield () -> service.updateStatus(status, USER, ORG);
        }
        default -> throw new AssertionError(operation);
        };

        assertThrows(GenericException.class, write);

        verify(plans, never()).insert(any());
        verify(plans, never()).update(any());
        verifyNoInteractions(fields, statistics, base);
    }
}
