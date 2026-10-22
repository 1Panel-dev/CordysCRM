package cn.cordys.common.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.approval.service.ApprovalResourceQueryService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseServiceTransactionTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();
    private TransactionInterceptor transactions;

    @BeforeEach
    void setUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        transactions = new TransactionInterceptor(new DataSourceTransactionManager(dataSource), attributes);
    }

    @Test
    void facadeAndUserQueriesShareOneWritableTransaction() throws Exception {
        ExtUserMapper mapper = mock(ExtUserMapper.class);
        UserQueryService users = new UserQueryService();
        ReflectionTestUtils.setField(users, "extUserMapper", mapper);
        BaseService target = new BaseService();
        ReflectionTestUtils.setField(target, "userQueryService", proxy(users));
        when(mapper.selectUserOptionByIds(List.of("user-1"))).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            return List.of(new OptionDTO("user-1", "用户名"));
        });

        assertEquals(Map.of("user-1", "用户名"), proxy(target).getUserNameMap(List.of("user-1")));

        verify(dataSource).getConnection();
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void logAssemblyFailureStillRollsBackTheFacadeTransaction() throws Exception {
        BaseMapper<ModuleField> mapper = mock(BaseMapper.class);
        when(mapper.selectListByLambda(any())).thenThrow(new IllegalStateException("field query failed"));
        ResourceLogService logs = new ResourceLogService();
        ReflectionTestUtils.setField(logs, "moduleFieldMapper", mapper);
        BaseService target = new BaseService();
        ReflectionTestUtils.setField(target, "resourceLogService", proxy(logs));

        assertThrows(IllegalStateException.class, () -> proxy(target).handleUpdateLog(
                Map.of("id", "resource-1"), Map.of("id", "resource-1"),
                List.of(new BaseModuleFieldValue("field-1", "old")), null, "resource-1", "资源"));

        verify(dataSource).getConnection();
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void delegatedLogSnapshotRemainsInTheCallingOperationSpan() throws Exception {
        BaseService target = new BaseService();
        ReflectionTestUtils.setField(target, "resourceLogService", proxy(new ResourceLogService()));
        Customer customer = new Customer();
        customer.setId("customer-1");
        customer.setName("客户名称");
        LogContextInfo parent = LogContextInfo.builder().resourceId("parent").build();
        OperationLogContext.putEmptySpan();
        OperationLogContext.setContext(parent);
        try {
            OperationLogContext.putEmptySpan();
            try {
                proxy(target).handleAddLogWithResourceName(customer, null);

                assertEquals("customer-1", OperationLogContext.getContext().getResourceId());
                assertEquals("客户名称", OperationLogContext.getContext().getResourceName());
            } finally {
                OperationLogContext.clear();
            }
            assertSame(parent, OperationLogContext.getContext());
        } finally {
            OperationLogContext.clear();
        }
        verify(connection).commit();
    }

    @Test
    void allExtractedServicesRetainCheckedExceptionRollback() throws Exception {
        for (Class<?> type : List.of(BaseService.class, UserQueryService.class, ResourceLogService.class,
                ApprovalResourceQueryService.class)) {
            var method = Arrays.stream(type.getDeclaredMethods())
                    .filter(candidate -> Modifier.isPublic(candidate.getModifiers())).findFirst().orElseThrow();
            var transaction = attributes.getTransactionAttribute(method, type);
            assertNotNull(transaction, type.getSimpleName());
            assertTrue(transaction.rollbackOn(new Exception("checked failure")), type.getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(transactions);
        return (T) factory.getProxy();
    }
}
