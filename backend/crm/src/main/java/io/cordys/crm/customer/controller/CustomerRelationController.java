package io.cordys.crm.customer.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import io.cordys.crm.customer.dto.response.CustomerRelationListResponse;
import io.cordys.crm.customer.service.CustomerRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "客户关系")
@RestController
@RequestMapping("/customer/relation")
public class CustomerRelationController {
    @Resource
    private CustomerRelationService customerRelationService;

    @GetMapping("/list/{customerId}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户关系列表")
    public List<CustomerRelationListResponse> list(@PathVariable String customerId) {
        return customerRelationService.list(customerId);
    }

    @PostMapping("/save/{customerId}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "添加客户关系")
    public void save(@PathVariable String customerId, @Validated @RequestBody @Valid @NotNull List<CustomerRelationSaveRequest> requests) {
        customerRelationService.save(customerId, requests);
    }
}
