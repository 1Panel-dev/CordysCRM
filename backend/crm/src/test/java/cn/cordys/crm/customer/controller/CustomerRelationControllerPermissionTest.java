package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomerRelationControllerPermissionTest {

    @Test
    void saveShouldCheckCustomerScope() throws NoSuchMethodException {
        var method = CustomerRelationController.class.getDeclaredMethod("save", String.class, List.class);
        var permission = method.getAnnotation(CsPermission.class);

        assertNotNull(permission);
        assertEquals(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, permission.value());
        assertEquals("{#customerId}", permission.resourceId());
        assertEquals(FormKeyConstants.CUSTOMER, permission.formType());
    }

    @Test
    void addShouldCheckCustomerScope() throws NoSuchMethodException {
        var method = CustomerRelationController.class.getDeclaredMethod("add", String.class,
                CustomerRelationSaveRequest.class);
        var permission = method.getAnnotation(CsPermission.class);

        assertNotNull(permission);
        assertEquals(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, permission.value());
        assertEquals("{#customerId}", permission.resourceId());
        assertEquals(FormKeyConstants.CUSTOMER, permission.formType());
    }
}
