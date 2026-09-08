package cn.cordys.common.formula;

import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FormulaReferenceValueServiceTest {
    @Test
    void 只解析同组织可读来源并逐一拒绝缺失越权和跨组织引用() {
        FormulaRecordSnapshotService snapshots = mock(FormulaRecordSnapshotService.class);
        ApplicationContext beans = mock(ApplicationContext.class);
        ResourcePermissionService permission = mock(ResourcePermissionService.class);
        DataScopeService scope = mock(DataScopeService.class);
        when(beans.getBean(ResourcePermissionService.class)).thenReturn(permission);
        when(beans.getBean(DataScopeService.class)).thenReturn(scope);
        FormulaReferenceValueService service = new FormulaReferenceValueService(snapshots, beans);
        DatasourceField field = new DatasourceField();
        field.setType("DATA_SOURCE");
        field.setDataSourceType("CUSTOMER");
        Customer customer = new Customer();
        customer.setOrganizationId("org-1");
        customer.setOwner("owner-1");
        customer.setName("可读客户");
        when(snapshots.readEntity("customer", "record-1")).thenReturn(customer);
        when(scope.hasDataPermission("user-1", "org-1", "owner-1", "CUSTOMER_MANAGEMENT:READ")).thenReturn(true);
        try (var session = mockStatic(SessionUtils.class); var organization = mockStatic(OrganizationContext.class)) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");
            assertEquals("可读客户", service.resolve(field, "record-1"));
            assertThrows(FormulaEvaluationException.class, () -> service.resolve(field, "missing"));
            when(scope.hasDataPermission("user-1", "org-1", "owner-1", "CUSTOMER_MANAGEMENT:READ")).thenReturn(false);
            assertThrows(FormulaEvaluationException.class, () -> service.resolve(field, "record-1"));
            when(scope.hasDataPermission("user-1", "org-1", "owner-1", "CUSTOMER_MANAGEMENT:READ")).thenReturn(true);
            field.setType("DATA_SOURCE_MULTIPLE");
            assertEquals("可读客户", service.resolve(field, List.of("record-1")));
            assertThrows(FormulaEvaluationException.class, () -> service.resolve(field, List.of("record-1", "missing")));
            customer.setOrganizationId("other-org");
            clearInvocations(permission);
            assertThrows(FormulaEvaluationException.class, () -> service.resolve(field, List.of("record-1")));
            verifyNoInteractions(permission);
        }
    }
}
