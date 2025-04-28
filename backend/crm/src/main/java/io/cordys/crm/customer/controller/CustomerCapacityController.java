package io.cordys.crm.customer.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.dto.CustomerCapacityDTO;
import io.cordys.crm.customer.service.CustomerCapacityService;
import io.cordys.crm.system.dto.request.CapacityAddRequest;
import io.cordys.crm.system.dto.request.CapacityUpdateRequest;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-capacity")
@Tag(name = "客户库容容量设置")
public class CustomerCapacityController {

    @Resource
    private CustomerCapacityService customerCapacityService;

    @GetMapping("/get")
    @Operation(summary = "获取客户库容设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public List<CustomerCapacityDTO> list() {
        return customerCapacityService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "添加客户库容设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void add(@Validated @RequestBody CapacityAddRequest request) {
        customerCapacityService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "修改客户库容设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody CapacityUpdateRequest request) {
        customerCapacityService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除客户库容设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void delete(@PathVariable("id") @Validated String id) {
        customerCapacityService.delete(id);
    }
}
