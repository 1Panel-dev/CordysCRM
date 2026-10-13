package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClueControllerPermissionTest {

    @Test
    void singleClueMoveShouldUseClueRecyclePermission() throws NoSuchMethodException {
        var method = ClueController.class.getDeclaredMethod("toPool", PoolReasonRequest.class);
        var permission = method.getAnnotation(CsPermission.class);

        assertNotNull(permission);
        assertEquals(PermissionConstants.CLUE_MANAGEMENT_RECYCLE, permission.value());
    }

    @Test
    void transitionCustomerListShouldCheckClueResourceReadPermission() throws NoSuchMethodException {
        var method = ClueController.class.getDeclaredMethod("transitionCustomerList", String.class);
        var permission = method.getAnnotation(CsPermission.class);
        var mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(permission);
        assertEquals(PermissionConstants.CLUE_MANAGEMENT_READ, permission.value());
        assertEquals("{#id}", permission.resourceId());
        assertEquals(FormKeyConstants.CLUE, permission.formType());
        assertNotNull(mapping);
        assertEquals("/transition/account/list/{id}", mapping.value()[0]);
    }
}
