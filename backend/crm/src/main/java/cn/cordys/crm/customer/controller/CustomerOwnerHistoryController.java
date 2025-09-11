package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.dto.response.CustomerOwnerListResponse;
import cn.cordys.crm.customer.service.CustomerOwnerHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "客户责任人历史")
@RestController
@RequestMapping("/account/owner/history")
public class CustomerOwnerHistoryController {
    @Resource
    private CustomerOwnerHistoryService customerOwnerHistoryService;

    @GetMapping("/list/{customerId}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    @Operation(summary = "客户责任人历史列表")
    public List<CustomerOwnerListResponse> list(@PathVariable String customerId) {
        return customerOwnerHistoryService.list(customerId, OrganizationContext.getOrganizationId());
    }
}
