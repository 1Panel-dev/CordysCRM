package cn.cordys.common.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.system.constants.FieldSourceType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldSourceServiceProviderTest {
    private final FieldSourceServiceProvider provider = new FieldSourceServiceProvider();
    private final Map<String, Object> beans = new HashMap<>();

    @BeforeEach
    void initializeReaders() {
        for (Field field : FieldSourceServiceProvider.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class)) {
                Object bean = mock(field.getType(), RETURNS_DEEP_STUBS);
                beans.put(field.getName(), bean);
                ReflectionTestUtils.setField(provider, field.getName(), bean);
            }
        }
        provider.init();
    }

    @ParameterizedTest
    @CsvSource({
            "CLUE,clueService",
            "CUSTOMER,customerService",
            "OPPORTUNITY,opportunityService",
            "CONTACT,customerContactService",
            "PRODUCT,productService",
            "PRICE,productPriceService",
            "QUOTATION,opportunityQuotationService",
            "CONTRACT,contractService",
            "BUSINESS_TITLE,businessTitleService",
            "ORDER,orderService",
            "CONTRACT_PAYMENT_RECORD,paymentRecordService",
            "PAYMENT_PLAN,paymentPlanService",
            "INVOICE,invoiceService",
            "CUSTOM_FORM,customFormDataService"
    })
    void routesSingleAndBatchQueriesToTheirOwnBusinessService(FieldSourceType type, String beanName) {
        Object bean = beans.get(beanName);
        Object detail = ReflectionTestUtils.invokeMethod(bean, "getSimple", "record-1");
        List<Object> batch = new ArrayList<>(List.of(detail));
        ReflectionTestUtils.invokeMethod(doReturn(batch).when(bean), "batchGetSimpleByIds", List.of("record-1"));

        assertSame(detail, provider.safeGetSimpleById(type, "record-1"));
        assertSame(batch, provider.batchGetSimpleByIds(type, List.of("record-1")));
    }

    @Test
    void approvalAndCustomFormRoutesKeepTheirSupportedOperations() {
        Object customer = ReflectionTestUtils.invokeMethod(beans.get("customerService"), "getSimple", "record-1");
        Object contractFields = ReflectionTestUtils.invokeMethod(beans.get("contractService"),
                "getFieldValues", "record-1");
        Object custom = ReflectionTestUtils.invokeMethod(beans.get("customFormDataService"), "getSimple", "record-1");
        Object customFields = ReflectionTestUtils.invokeMethod(beans.get("customFormDataService"),
                "getFieldValues", "record-1");

        assertSame(customer, provider.safeGetSimpleById(FormKey.CUSTOMER.getKey(), "record-1"));
        assertSame(contractFields, provider.safeGetFieldsById(FormKey.CONTRACT.getKey(), "record-1"));
        assertSame(custom, provider.safeGetSimpleById("custom-form-1", "record-1"));
        assertSame(customFields, provider.safeGetFieldsById("custom-form-1", "record-1"));
        assertNull(provider.safeGetSimpleById(FormKey.PRODUCT.getKey(), "record-1"));
        assertNull(provider.safeGetFieldsById(FormKey.CUSTOMER.getKey(), "record-1"));
        assertNull(provider.safeGetSimpleById("CUSTOMER", "record-1"));
    }

    @Test
    void missingIdsAndDownstreamFailuresRetainNullOrEmptyResults() {
        Object customer = beans.get("customerService");
        ReflectionTestUtils.invokeMethod(doThrow(new IllegalStateException("查询失败")).when(customer),
                "getSimple", "record-1");
        ReflectionTestUtils.invokeMethod(doThrow(new IllegalStateException("查询失败")).when(customer),
                "batchGetSimpleByIds", List.of("record-1"));

        assertNull(provider.safeGetSimpleById(FieldSourceType.CUSTOMER, "record-1"));
        assertEquals(List.of(), provider.batchGetSimpleByIds(FieldSourceType.CUSTOMER, List.of("record-1")));
        assertNull(provider.safeGetSimpleById(FieldSourceType.CUSTOMER, null));
        assertNull(provider.safeGetSimpleById(FieldSourceType.CUSTOMER, ""));
        assertNull(provider.batchGetSimpleByIds(FieldSourceType.CUSTOMER, null));
        assertEquals(List.of(), provider.batchGetSimpleByIds(FieldSourceType.CUSTOMER, List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "source", "form", "fields", "batch" })
    void failedReferenceTransactionDoesNotRollBackTheCaller(String operation) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        List<Connection> connections = new ArrayList<>();
        when(dataSource.getConnection()).thenAnswer(invocation -> {
            Connection connection = mock(Connection.class);
            when(connection.getAutoCommit()).thenReturn(true);
            connections.add(connection);
            return connection;
        });
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(dataSource);
        Object bean = beans.get(operation.equals("fields") ? "contractService" : "customerService");
        Object stub = doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return new TransactionTemplate(transactions).execute(status -> {
                throw new IllegalStateException("独立查询事务失败");
            });
        }).when(bean);
        String method = switch (operation) {
        case "fields" -> "getFieldValues";
        case "batch" -> "batchGetSimpleByIds";
        default -> "getSimple";
        };
        Object argument = operation.equals("batch") ? List.of("record-1") : "record-1";
        ReflectionTestUtils.invokeMethod(stub, method, argument);

        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(provider);
        factory.setProxyTargetClass(true);
        factory.addAdvice(interceptor);
        FieldSourceServiceProvider proxy = (FieldSourceServiceProvider) factory.getProxy();

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Object result = switch (operation) {
            case "source" -> proxy.safeGetSimpleById(FieldSourceType.CUSTOMER, "record-1");
            case "form" -> proxy.safeGetSimpleById(FormKey.CUSTOMER.getKey(), "record-1");
            case "fields" -> proxy.safeGetFieldsById(FormKey.CONTRACT.getKey(), "record-1");
            default -> proxy.batchGetSimpleByIds(FieldSourceType.CUSTOMER, List.of("record-1"));
            };
            if (operation.equals("batch")) {
                assertEquals(List.of(), result);
            } else {
                assertNull(result);
            }
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(status.isRollbackOnly());
        });

        assertEquals(2, connections.size());
        verify(connections.get(0)).commit();
        verify(connections.get(1)).rollback();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }
}
