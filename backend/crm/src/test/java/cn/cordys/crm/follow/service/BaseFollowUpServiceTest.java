package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BaseFollowUpServiceTest {
    private final BaseFollowUpService service = new BaseFollowUpService();
    private final ResourcePermissionService permissions = mock(ResourcePermissionService.class);
    private final RelatedResourcePermissionService relatedPermissions = mock(RelatedResourcePermissionService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);

    @BeforeEach
    void 配置权限依赖() {
        ReflectionTestUtils.setField(service, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(service, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
    }

    @ParameterizedTest
    @ValueSource(strings = { FormKeyConstants.FOLLOW_RECORD, FormKeyConstants.FOLLOW_PLAN })
    void 隔离数据不能通过父客户读取权限绕过(String formType) {
        FollowUpRecord record = 父客户可读的跟进(formType);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(relatedPermissions)
                .checkIsolatedData(formType, "follow-a", "receiver-a", "org-a");

        GenericException exception = assertThrows(GenericException.class,
                () -> service.checkRelatedReadPermission(record, "org-a", "receiver-a", permission(formType), formType));

        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(permissions, customers);
    }

    @ParameterizedTest
    @ValueSource(strings = { FormKeyConstants.FOLLOW_RECORD, FormKeyConstants.FOLLOW_PLAN })
    void 非隔离数据仍可通过父客户读取权限查看(String formType) {
        FollowUpRecord record = 父客户可读的跟进(formType);

        assertDoesNotThrow(
                () -> service.checkRelatedReadPermission(record, "org-a", "receiver-a", permission(formType), formType));

        verify(relatedPermissions).checkIsolatedData(formType, "follow-a", "receiver-a", "org-a");
        verify(permissions).checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ, "customer-a",
                FormKeyConstants.CUSTOMER, "receiver-a", "org-a");
    }

    @Test
    void 不存在的跟进返回未找到() {
        GenericException exception = assertThrows(GenericException.class,
                () -> service.checkRelatedReadPermission(null, "org-a", "receiver-a",
                        PermissionConstants.FOLLOW_RECORD_READ, FormKeyConstants.FOLLOW_RECORD));

        assertEquals(CrmHttpResultCode.NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(permissions, relatedPermissions, customers);
    }

    private FollowUpRecord 父客户可读的跟进(String formType) {
        FollowUpRecord record = new FollowUpRecord();
        record.setId("follow-a");
        record.setCustomerId("customer-a");
        record.setOrganizationId("org-a");
        record.setOwner("collaborator-a");
        Customer customer = new Customer();
        customer.setId("customer-a");
        customer.setOrganizationId("org-a");
        customer.setOwner("receiver-a");
        customer.setInSharedPool(false);
        when(customers.selectByPrimaryKey("customer-a")).thenReturn(customer);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkResourcePermission(permission(formType), "follow-a", formType, "receiver-a", "org-a");
        return record;
    }

    private String permission(String formType) {
        return FormKeyConstants.FOLLOW_RECORD.equals(formType)
                ? PermissionConstants.FOLLOW_RECORD_READ : PermissionConstants.FOLLOW_PLAN_READ;
    }
}
