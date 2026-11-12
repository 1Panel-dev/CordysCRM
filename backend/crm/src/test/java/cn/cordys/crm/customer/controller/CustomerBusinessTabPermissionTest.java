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
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerInvoiceStatisticResponse;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractService;
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
    void 配置当前用户及权限依赖() {
        sessions = mockStatic(SessionUtils.class);
        sessions.when(SessionUtils::getUserId).thenReturn("user-a");
        organizations = mockStatic(OrganizationContext.class);
        organizations.when(OrganizationContext::getOrganizationId).thenReturn("org-a");
        ReflectionTestUtils.setField(controller, "relatedResourcePermissionService", related);
        ReflectionTestUtils.setField(controller, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(controller, "dataScopeService", dataScope);
    }

    @AfterEach
    void 恢复静态会话() {
        organizations.close();
        sessions.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void 六类业务标签采用协作范围但保留功能权限(boolean self) {
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
    void 客户负责人沿用关联业务的功能授权() {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(true);
        when(related.getCustomerTabDataPermission("customer-a", "user-a", "org-a")).thenReturn(scope);
        when(related.isOwner(FormKey.CUSTOMER.getKey(), "customer-a", "user-a", "org-a")).thenReturn(true);

        assertSame(scope, ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                "customer-a", "ALL", PermissionConstants.CONTRACT_READ));
        verifyNoInteractions(permissions, dataScope);
    }

    @Test
    void 协作成员缺少业务查看功能权限时拒绝() {
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
    void 非客户负责人及协作成员仍使用业务角色数据范围() {
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        when(dataScope.getDeptDataPermission("user-a", "org-a", "ALL", PermissionConstants.CONTRACT_READ))
                .thenReturn(scope);

        assertSame(scope, ReflectionTestUtils.invokeMethod(controller, "getRelatedDataPermission",
                "customer-a", "ALL", PermissionConstants.CONTRACT_READ));
        verify(permissions).checkPermission(PermissionConstants.CONTRACT_READ);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void 订单统计固定关联客户并采用标签范围(boolean self) {
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
    void 客户发票统计分别传递发票及合同的可见范围() {
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
}
