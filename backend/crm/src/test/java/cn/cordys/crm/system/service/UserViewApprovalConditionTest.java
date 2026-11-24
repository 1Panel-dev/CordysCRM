package cn.cordys.crm.system.service;

import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.domain.UserViewCondition;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtUserViewConditionMapper;
import cn.cordys.crm.system.mapper.ExtUserViewMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserViewApprovalConditionTest {

    private final UserViewService service = new UserViewService();
    private final BaseMapper<UserView> views = mock(BaseMapper.class);
    private final BaseMapper<UserViewCondition> conditions = mock(BaseMapper.class);
    private final ExtUserViewMapper extViews = mock(ExtUserViewMapper.class);
    private final ExtUserViewConditionMapper extConditions = mock(ExtUserViewConditionMapper.class);
    private final ApprovalFlowService flows = mock(ApprovalFlowService.class);
    private final ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
    private final ModuleFormService formService = mock(ModuleFormService.class);
    private final UserView view = new UserView();
    private final List<UserViewCondition> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userViewMapper", views);
        ReflectionTestUtils.setField(service, "userViewConditionMapper", conditions);
        ReflectionTestUtils.setField(service, "extUserViewMapper", extViews);
        ReflectionTestUtils.setField(service, "extUserViewConditionMapper", extConditions);
        ReflectionTestUtils.setField(service, "approvalFlowService", flows);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", forms);
        ReflectionTestUtils.setField(service, "moduleFormService", formService);
        view.setId("view-1");
        view.setResourceType("CUSTOMER");
        view.setOrganizationId("org-1");
        view.setSearchMode("AND");
        when(views.selectByPrimaryKey("view-1")).thenReturn(view);
        when(extViews.countUserView("user-1", "view-1", "org-1")).thenReturn(1);
        when(extConditions.getViewConditions("view-1")).thenAnswer(ignored -> List.copyOf(stored));
        stored.add(storedCondition("approvalStatus", "APPROVING"));
        stored.add(storedCondition("name", "客户"));
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(new ArrayList<>());
        when(forms.getBusinessFormConfig("customer", "org-1")).thenReturn(config);
        when(formService.getOptionMap(any(), anyList())).thenReturn(Map.of());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void detailKeepsApprovalConditionWhenFlowIsEnabledOrRecordsAreApproving(boolean flowEnabled) {
        assertEquals(List.of("name"), names(service.getViewDetail("view-1", "user-1", "org-1", "customer")
                .getConditions()));

        if (flowEnabled) {
            when(flows.getEnabledFlow("customer", "org-1")).thenReturn(new ApprovalFlow());
        } else {
            when(flows.existsApprovingData("customer", "org-1", null)).thenReturn(true);
        }

        List<FilterCondition> restored = service.getViewDetail("view-1", "user-1", "org-1", "customer")
                .getConditions();
        assertEquals(List.of("approvalStatus", "name"), names(restored));
        assertEquals(List.of("APPROVING"), restored.getFirst().getValue());
        verifyNoInteractions(conditions);
    }

    @Test
    void disabledApprovalOnlyViewHasNoEffectiveConditions() {
        stored.removeLast();
        assertTrue(service.getFilterConditions("view-1").isEmpty());
        assertEquals(1, stored.size());
    }

    @Test
    void viewWithoutApprovalConditionDoesNotLoadApprovalConfiguration() {
        stored.removeFirst();
        assertEquals(List.of("name"), names(service.getFilterConditions("view-1")));
        verifyNoInteractions(views, flows);
    }

    @ParameterizedTest
    @CsvSource({
            "CUSTOMER_POOL,customer", "CLUE_POOL,clue", "OPPORTUNITY_QUOTATION,quotation",
            "CONTRACT_INVOICE,invoice", "QUOTATION,quotation", "INVOICE,invoice", "customForm-1,customForm-1",
            "CONTRACT_PAYMENT_PLAN,contractPaymentPlan", "CONTRACT_PAYMENT_RECORD,contractPaymentRecord",
            "FOLLOW_PLAN,plan", "FOLLOW_RECORD,record", "CUSTOMER_CONTACT,contact"
    })
    void resolvesApprovalConfigurationForViewResource(String resourceType, String formKey) {
        view.setResourceType(resourceType);
        when(flows.getEnabledFlow(formKey, "org-1")).thenReturn(new ApprovalFlow());

        assertEquals(List.of("approvalStatus", "name"), names(service.getFilterConditions("view-1")));
        verify(flows).getEnabledFlow(formKey, "org-1");
    }

    @ParameterizedTest
    @CsvSource({ "CLUE_POOL,clue,cluePool", "CUSTOMER_POOL,customer,customerOpenSea" })
    void disabledPoolViewUsesPoolRecordsToKeepApprovalCondition(String resourceType, String configFormType,
            String dataFormType) {
        view.setResourceType(resourceType);
        when(flows.existsApprovingData(dataFormType, "org-1", null)).thenReturn(true);

        assertEquals(List.of("approvalStatus", "name"), names(service.getFilterConditions("view-1")));
        verify(flows).getEnabledFlow(configFormType, "org-1");
        verify(flows).existsApprovingData(dataFormType, "org-1", null);
    }

    @Test
    void savingPreservesHiddenConditionAndAllowsDeletionWhenRecordsAreApproving() {
        doAnswer(ignored -> {
            stored.clear();
            return null;
        }).when(extConditions).delete("view-1");
        doAnswer(invocation -> {
            stored.addAll(invocation.<List<UserViewCondition>>getArgument(0));
            return null;
        }).when(conditions).batchInsert(anyList());
        UserViewUpdateRequest request = new UserViewUpdateRequest();
        request.setId("view-1");
        request.setName("编辑后的视图");
        request.setConditions(service.getFilterConditions("view-1"));

        try (MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class)) {
            ids.when(IDGenerator::nextStr).thenReturn("condition-1");
            service.update(request, "user-1", "org-1");
            service.update(request, "user-1", "org-1");

            assertEquals(2, stored.size());
            assertEquals(1, stored.stream().filter(condition -> "approvalStatus".equals(condition.getName())).count());
            assertEquals(List.of("name"), names(service.getFilterConditions("view-1")));

            when(flows.existsApprovingData("customer", "org-1", null)).thenReturn(true);
            List<FilterCondition> restored = service.getFilterConditions("view-1");
            assertEquals(List.of("name", "approvalStatus"), names(restored));
            assertEquals(List.of("APPROVING"), restored.getLast().getValue());

            request.setConditions(restored.stream().filter(condition -> !"approvalStatus".equals(condition.getName()))
                    .toList());
            service.update(request, "user-1", "org-1");
            assertEquals(List.of("name"), names(service.getFilterConditions("view-1")));
        }
    }

    @ParameterizedTest
    @CsvSource({ "AND,true", "AND,false", "OR,true", "OR,false" })
    void clueQueryUsesApprovalConditionWhenFlowIsEnabledOrRecordsAreApproving(String searchMode,
            boolean flowEnabled) {
        view.setResourceType("CLUE");
        view.setSearchMode(searchMode);
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of());
        when(forms.getBusinessFormConfig("clue", "org-1")).thenReturn(config);
        DepartmentService departments = mock(DepartmentService.class);
        when(departments.getTree("org-1")).thenReturn(List.of());
        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class);
                MockedStatic<OrganizationContext> organization = mockStatic(OrganizationContext.class)) {
            beans.when(() -> CommonBeanFactory.getBean(UserViewService.class)).thenReturn(service);
            beans.when(() -> CommonBeanFactory.getBean(ModuleFormCacheService.class)).thenReturn(forms);
            beans.when(() -> CommonBeanFactory.getBean(DepartmentService.class)).thenReturn(departments);
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            BaseCondition request = new BaseCondition();
            request.setViewId("view-1");
            ConditionFilterUtils.parseCondition(request, "clue");
            assertEquals(List.of("name"), names(request.getViewCombineSearch().getConditions()));
            assertEquals(searchMode, request.getViewCombineSearch().getSearchMode());

            if (flowEnabled) {
                when(flows.getEnabledFlow("clue", "org-1")).thenReturn(new ApprovalFlow());
            } else {
                when(flows.existsApprovingData("clue", "org-1", null)).thenReturn(true);
            }
            ConditionFilterUtils.parseCondition(request, "clue");
            assertEquals(List.of("approvalStatus", "name"), names(request.getViewCombineSearch().getConditions()));
            assertEquals(searchMode, request.getViewCombineSearch().getSearchMode());
            assertEquals(List.of("APPROVING"), request.getViewCombineSearch().getConditions().getFirst().getValue());
        }
    }

    private UserViewCondition storedCondition(String name, String value) {
        UserViewCondition condition = new UserViewCondition();
        condition.setName(name);
        condition.setValue(value);
        condition.setValueType("STRING");
        condition.setType("INPUT");
        condition.setOperator("EQUALS");
        if ("approvalStatus".equals(name)) {
            condition.setValue("[\"" + value + "\"]");
            condition.setValueType("ARRAY");
            condition.setType("SELECT_MULTIPLE");
            condition.setOperator("IN");
            condition.setMultipleValue(true);
        }
        return condition;
    }

    private List<String> names(List<FilterCondition> list) {
        return list.stream().map(FilterCondition::getName).toList();
    }
}
