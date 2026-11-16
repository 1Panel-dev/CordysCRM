package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.dto.response.CustomerPaymentPlanStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentRecordStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerInvoiceStatisticResponse;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.dto.request.CustomerContractPaymentPlanPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerContractPaymentRecordStatisticRequest;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerBusinessTabPermissionTest {
    private final CustomerController controller = new CustomerController();
    private final RelatedResourcePermissionService related = mock(RelatedResourcePermissionService.class);
    private final ResourcePermissionService permissions = mock(ResourcePermissionService.class);
    private final DataScopeService dataScope = mock(DataScopeService.class);
    private MockedStatic<SessionUtils> sessions;
    private MockedStatic<OrganizationContext> organizations;

    @BeforeEach
    void setUpCurrentUserAndPermissionDependencies() {
        sessions = mockStatic(SessionUtils.class);
        sessions.when(SessionUtils::getUserId).thenReturn("user-a");
        organizations = mockStatic(OrganizationContext.class);
        organizations.when(OrganizationContext::getOrganizationId).thenReturn("org-a");
        ReflectionTestUtils.setField(controller, "relatedResourcePermissionService", related);
        ReflectionTestUtils.setField(controller, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(controller, "dataScopeService", dataScope);
    }

    @AfterEach
    void closeStaticSessions() {
        organizations.close();
        sessions.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void businessTabsUseCollaborativeScopeAndRetainFunctionalPermissions(boolean self) {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(!self);
        scope.setSelf(self);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);

        for (String permission : List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
                PermissionConstants.CONTRACT_READ, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ,
                PermissionConstants.CONTRACT_PAYMENT_RECORD_READ, PermissionConstants.CONTRACT_INVOICE_READ,
                PermissionConstants.ORDER_READ)) {
            assertSame(scope, ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                    "customer-a", "ALL", permission));
            verify(permissions).checkPermission(permission);
        }
        verifyNoInteractions(dataScope);
    }

    @Test
    void customerOwnerUsesRelatedBusinessFunctionalPermission() {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        when(related.isOwner(FormKey.CUSTOMER.getKey(), "customer-a", "user-a", "org-a")).thenReturn(true);

        assertSame(scope, ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                "customer-a", "ALL", PermissionConstants.CONTRACT_READ));
        verifyNoInteractions(permissions, dataScope);
    }

    @Test
    void collaborativeMemberWithoutBusinessReadPermissionIsDenied() {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkPermission(PermissionConstants.CONTRACT_READ);

        GenericException exception = assertThrows(GenericException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                        "customer-a", "ALL", PermissionConstants.CONTRACT_READ));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(dataScope);
    }

    @Test
    void nonOwnerAndNonCollaboratorUsesBusinessRoleDataScope() {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        when(dataScope.getDeptDataPermission("user-a", "org-a", "ALL", PermissionConstants.CONTRACT_READ))
                .thenReturn(scope);

        assertSame(scope, ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                "customer-a", "ALL", PermissionConstants.CONTRACT_READ));
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_READ);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void orderStatisticFixesRelatedCustomerAndUsesTabScope(boolean self) {
        OrderService orders = mock(OrderService.class);
        ReflectionTestUtils.setField(controller, "orderService", orders);
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(!self);
        scope.setSelf(self);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        BaseCondition request = new BaseCondition();
        request.setViewId("role-view");
        request.setKeyword("订单");
        FilterCondition filter = new FilterCondition();
        filter.setName("customerId");
        filter.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        filter.setValue("other-customer");
        request.setFilters(List.of(filter));
        OrderStatisticResponse response = new OrderStatisticResponse();
        when(orders.searchStatistic(request, "user-a", "org-a", scope)).thenReturn(response);

        try (MockedStatic<ConditionFilterUtils> conditions = mockStatic(ConditionFilterUtils.class)) {
            conditions.when(() -> ConditionFilterUtils.getValidConditions(any()))
                    .thenCallRealMethod();

            assertSame(response, controller.calculateCustomerOrderStatistic("customer-a", request));

            conditions.verify(() -> ConditionFilterUtils.parseCondition(request, FormKey.ORDER.getKey()));
            assertEquals("ALL", request.getViewId());
            assertEquals("订单", request.getKeyword());
            assertEquals(2, request.getFilters().size());
            assertSame(filter, request.getFilters().getFirst());
            FilterCondition customer = request.getFilters().getLast();
            assertEquals("customerId", customer.getName());
            assertEquals("EQUALS", customer.getOperator());
            assertEquals("customer-a", customer.getValue());
        }
        verify(orders).searchStatistic(request, "user-a", "org-a", scope);
        verify(permissions).checkPermission(PermissionConstants.ORDER_READ);
        verifyNoInteractions(dataScope);
    }

    @Test
    void customerInvoiceStatisticPassesInvoiceAndContractVisibilityScopes() {
        ContractInvoiceService invoices = mock(ContractInvoiceService.class);
        ContractService contracts = mock(ContractService.class);
        ReflectionTestUtils.setField(controller, "contractInvoiceService", invoices);
        ReflectionTestUtils.setField(controller, "contractService", contracts);
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setSelf(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        when(invoices.calculateCustomerInvoiceAmount("customer-a", "user-a", "org-a", scope))
                .thenReturn(BigDecimal.TEN);
        CustomerContractStatisticResponse total = new CustomerContractStatisticResponse();
        total.setTotalAmount(BigDecimal.valueOf(100));
        when(contracts.calculateContractStatisticByCustomerId("customer-a", "user-a", "org-a", scope))
                .thenReturn(total);

        CustomerInvoiceStatisticResponse response = controller.calculateCustomerInvoiceStatistic("customer-a");

        assertEquals(BigDecimal.TEN, response.getInvoicedAmount());
        assertEquals(BigDecimal.valueOf(90), response.getUninvoicedAmount());
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_INVOICE_READ);
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_READ);
        verifyNoInteractions(dataScope);
    }

    @Test
    void customerPaymentPlanStatisticParsesFiltersAndUsesRelatedPermissionScope() {
        ContractPaymentPlanService paymentPlans = mock(ContractPaymentPlanService.class);
        ReflectionTestUtils.setField(controller, "contractPaymentPlanService", paymentPlans);
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        CustomerContractPaymentPlanPageRequest request = new CustomerContractPaymentPlanPageRequest();
        request.setCustomerId("customer-a");
        request.setKeyword("计划");
        request.setFilters(List.of(filter("planStatus", "COMPLETED")));
        CustomerPaymentPlanStatisticResponse response = new CustomerPaymentPlanStatisticResponse();
        when(paymentPlans.calculateCustomerPaymentPlanStatistic(request, "user-a", "org-a", scope))
                .thenReturn(response);

        try (MockedStatic<ConditionFilterUtils> conditions = mockStatic(ConditionFilterUtils.class)) {
            conditions.when(() -> ConditionFilterUtils.getValidConditions(any())).thenCallRealMethod();

            assertSame(response, controller.calculateCustomerPaymentPlanStatistic(request));

            conditions.verify(() -> ConditionFilterUtils.parseCondition(request, FormKey.CONTRACT_PAYMENT_PLAN.getKey()));
        }
        assertEquals("ALL", request.getViewId());
        verify(paymentPlans).calculateCustomerPaymentPlanStatistic(request, "user-a", "org-a", scope);
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        verifyNoInteractions(dataScope);
    }

    @Test
    void customerPaymentRecordStatisticParsesFiltersAndUsesRelatedPermissionScope() {
        ContractPaymentRecordService paymentRecords = mock(ContractPaymentRecordService.class);
        ReflectionTestUtils.setField(controller, "contractPaymentRecordService", paymentRecords);
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setSelf(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        CustomerContractPaymentRecordStatisticRequest request = new CustomerContractPaymentRecordStatisticRequest();
        request.setCustomerId("customer-a");
        request.setKeyword("回款");
        request.setFilters(List.of(filter("recordAmount", "100")));
        CustomerPaymentRecordStatisticResponse response = new CustomerPaymentRecordStatisticResponse();
        when(paymentRecords.sumCustomerPaymentAmount(request, "user-a", "org-a", scope)).thenReturn(response);

        try (MockedStatic<ConditionFilterUtils> conditions = mockStatic(ConditionFilterUtils.class)) {
            conditions.when(() -> ConditionFilterUtils.getValidConditions(any())).thenCallRealMethod();

            assertSame(response, controller.calculateCustomerPaymentRecordStatistic(request));

            conditions.verify(() -> ConditionFilterUtils.parseCondition(request,
                    FormKey.CONTRACT_PAYMENT_RECORD.getKey()));
        }
        assertEquals("ALL", request.getViewId());
        verify(paymentRecords).sumCustomerPaymentAmount(request, "user-a", "org-a", scope);
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        verifyNoInteractions(dataScope);
    }

    private FilterCondition filter(String name, String value) {
        FilterCondition filter = new FilterCondition();
        filter.setName(name);
        filter.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        filter.setValue(value);
        return filter;
    }
}
