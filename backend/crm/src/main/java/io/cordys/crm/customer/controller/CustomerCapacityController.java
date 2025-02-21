package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.dto.CustomerCapacityDTO;
import io.cordys.crm.customer.service.CustomerCapacityService;
import io.cordys.crm.system.dto.request.CapacityRequest;
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

    @PostMapping("/save")
    @Operation(summary = "保存客户库容设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void save(@Validated @RequestBody List<CapacityRequest> capacities) {
        customerCapacityService.save(capacities, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
