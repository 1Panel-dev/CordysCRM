package cn.cordys.common.service;

import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class CollaborationViewDataScopeTest {

    static Stream<Arguments> viewsAndPermissions() {
        return Stream.of("CUSTOMER_COLLABORATION", "COLLABORATION_VISIBLE", "ALL")
                .flatMap(view -> Stream.of(
                        PermissionConstants.CUSTOMER_MANAGEMENT_READ,
                        PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
                        PermissionConstants.CONTRACT_READ,
                        PermissionConstants.ORDER_READ,
                        PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ,
                        PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ,
                        PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                        PermissionConstants.CONTRACT_UPDATE,
                        PermissionConstants.CLUE_MANAGEMENT_READ)
                        .map(permission -> Arguments.of(view, permission)));
    }

    @ParameterizedTest
    @MethodSource("viewsAndPermissions")
    void onlyMatchingModuleReadViewsUseCollaborationScope(String view, String permission) {
        DataScopeService service = spy(new DataScopeService());
        DeptDataPermissionDTO roleScope = new DeptDataPermissionDTO();
        roleScope.setInvisible(true);
        doReturn(roleScope).when(service).getDeptDataPermission("reader-a", "org-a", permission);

        DeptDataPermissionDTO result = service.getDeptDataPermission("reader-a", "org-a", view, permission);

        boolean collaborative = "CUSTOMER_COLLABORATION".equals(view)
                && PermissionConstants.CUSTOMER_MANAGEMENT_READ.equals(permission)
                || "COLLABORATION_VISIBLE".equals(view) && List.of(
                        PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
                        PermissionConstants.CONTRACT_READ,
                        PermissionConstants.ORDER_READ).contains(permission);
        assertEquals(collaborative, result.getVisible());
        assertEquals(view, result.getViewId());
        assertTrue(InternalUserView.isInternalUserView(view));
        if (collaborative) {
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertFalse(result.getInvisible());
            assertTrue(result.getDeptIds().isEmpty());
        } else {
            assertSame(roleScope, result, "不匹配的模块及写权限必须保留原角色数据范围");
            assertTrue(result.getInvisible());
        }
    }
}
