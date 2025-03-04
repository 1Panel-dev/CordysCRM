package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.FormKey;
import io.cordys.crm.system.dto.response.BusinessModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;

import io.cordys.common.pager.Pager;
import io.cordys.security.SessionUtils;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.*;

import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.common.pager.PageUtils;
import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Tag(name = "客户联系人")
@RestController
@RequestMapping("/customer_contact")
public class CustomerContactController {
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "获取表单配置")
    public BusinessModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "客户联系人列表")
    public Pager<List<CustomerContactListResponse>> list(@Validated @RequestBody CustomerContactPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, customerContactService.list(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "客户联系人详情")
    public CustomerContactGetResponse get(@PathVariable String id){
        return customerContactService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_ADD)
    @Operation(summary = "添加客户联系人")
    public CustomerContact add(@Validated @RequestBody CustomerContactAddRequest request) {
		return customerContactService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE)
    @Operation(summary = "更新客户联系人")
    public CustomerContact update(@Validated @RequestBody CustomerContactUpdateRequest request) {
        return customerContactService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE)
    @Operation(summary = "删除客户联系人")
    public void delete(@PathVariable String id) {
		customerContactService.delete(id);
    }
}
