package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.dto.CustomerCapacityDTO;
import io.cordys.crm.customer.dto.request.CustomerCapacitySaveRequest;
import io.cordys.crm.customer.service.CustomerCapacityService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-capacity")
@Tag(name = "客户库容容量")
public class CustomerCapacityController {

    @Resource
    private CustomerCapacityService customerCapacityService;

    @PostMapping("/page")
    @Operation(summary = "分页获取客户库容规则")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public Pager<List<CustomerCapacityDTO>> page(@Validated @RequestBody BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, customerCapacityService.page(OrganizationContext.getOrganizationId()));
    }

    @PostMapping("/save")
    @Operation(summary = "保存客户库容规则")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void save(@Validated @RequestBody CustomerCapacitySaveRequest request) {
        customerCapacityService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
