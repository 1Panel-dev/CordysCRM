package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class OpportunityDetailOperationPermissionTest {
    private static final String OPPORTUNITY_ID = "opportunity";
    private static final String USER_ID = "viewer";
    private static final String ORGANIZATION_ID = "organization";

    private final OpportunityService service = spy(new OpportunityService());
    private final ResourcePermissionService permissionService = mock(ResourcePermissionService.class);

    @BeforeEach
    void setUp() {
        OpportunityDetailResponse detail = new OpportunityDetailResponse();
        detail.setId(OPPORTUNITY_ID);
        detail.setOwner("owner");
        doReturn(detail).when(service).get(OPPORTUNITY_ID);
        ReflectionTestUtils.setField(service, "resourcePermissionService", permissionService);
    }

    @Test
    void returnsOnlyAllowedOperationPermissions() {
        denyPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_TRANSFER);

        OpportunityDetailResponse detail = service.getWithDataPermissionCheck(OPPORTUNITY_ID, USER_ID, ORGANIZATION_ID);

        assertEquals(List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE,
                PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE), detail.getOperationPermissions());
    }

    @Test
    void preservesReadableDetailWhenNoOperationIsAllowed() {
        denyPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE);
        denyPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_TRANSFER);
        denyPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE);

        OpportunityDetailResponse detail = service.getWithDataPermissionCheck(OPPORTUNITY_ID, USER_ID, ORGANIZATION_ID);

        assertEquals(OPPORTUNITY_ID, detail.getId());
        assertEquals(List.of(), detail.getOperationPermissions());
    }

    @Test
    void propagatesUnexpectedPermissionCheckFailure() {
        GenericException failure = new GenericException(CrmHttpResultCode.FAILED);
        doThrow(failure).when(permissionService).checkResourcePermissionWithoutApprovalTask(
                PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE,
                OPPORTUNITY_ID, FormKeyConstants.OPPORTUNITY, USER_ID, ORGANIZATION_ID);

        assertSame(failure, assertThrows(GenericException.class,
                () -> service.getWithDataPermissionCheck(OPPORTUNITY_ID, USER_ID, ORGANIZATION_ID)));
    }

    private void denyPermission(String permission) {
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissionService)
                .checkResourcePermissionWithoutApprovalTask(
                        permission, OPPORTUNITY_ID, FormKeyConstants.OPPORTUNITY, USER_ID, ORGANIZATION_ID);
    }
}
