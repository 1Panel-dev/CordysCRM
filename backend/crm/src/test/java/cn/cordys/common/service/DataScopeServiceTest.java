package cn.cordys.common.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.dto.RolePermissionDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class DataScopeServiceTest {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void departmentScopeFromUnrelatedPermissionDoesNotOverrideReadScope(boolean canReadOwn) {
        RolePermissionDTO department = new RolePermissionDTO();
        department.setPermissions(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT));
        RolePermissionDTO self = new RolePermissionDTO();
        self.setPermissions(canReadOwn ? Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ) : Set.of());
        DataScopeService dataScopeService = spy(new DataScopeService());
        doReturn(Map.of(RoleDataScope.DEPT_CUSTOM.name(), List.of(department),
                RoleDataScope.SELF.name(), List.of(self)))
                .when(dataScopeService).getDataScopeRoleMap("member", "org");

        var scope = dataScopeService.getDeptDataPermission("member", "org",
                PermissionConstants.CUSTOMER_MANAGEMENT_READ);

        assertEquals(canReadOwn, scope.getSelf());
        assertEquals(!canReadOwn, scope.getInvisible());
        assertTrue(scope.getDeptIds().isEmpty());
    }
}
