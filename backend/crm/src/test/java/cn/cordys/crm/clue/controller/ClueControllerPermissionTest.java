package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import org.junit.jupiter.api.Test;

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
}
