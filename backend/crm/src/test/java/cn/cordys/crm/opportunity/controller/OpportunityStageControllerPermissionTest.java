package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.opportunity.dto.request.OpportunityStageRequest;
import cn.cordys.common.constants.FormKeyConstants;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;

import static org.junit.jupiter.api.Assertions.*;

class OpportunityStageControllerPermissionTest {
    @Test
    void stageUpdateRequiresResourcePermissionAndValidRequest() throws NoSuchMethodException {
        var method = OpportunityController.class.getDeclaredMethod("updateStage", OpportunityStageRequest.class);
        var permission = method.getAnnotation(CsPermission.class);
        assertNotNull(permission);
        assertEquals(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, permission.value());
        assertEquals("{#request.id}", permission.resourceId());
        assertEquals(FormKeyConstants.OPPORTUNITY, permission.formType());
        var roles = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(roles);
        assertEquals(Logical.OR, roles.logical());
        assertArrayEquals(new String[]{PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE,
                PermissionConstants.OPPORTUNITY_MANAGEMENT_RESIGN}, roles.value());
        assertTrue(method.getParameters()[0].isAnnotationPresent(Validated.class));
    }
}
