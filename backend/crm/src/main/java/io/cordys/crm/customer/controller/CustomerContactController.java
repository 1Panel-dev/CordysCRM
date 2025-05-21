package io.cordys.crm.customer.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.DataScopeService;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import io.cordys.crm.customer.dto.request.CustomerContactDisableRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Tag(name = "客户联系人")
@RestController
@RequestMapping("/customer/contact")
public class CustomerContactController {
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/module/form")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人列表")
    public PagerWithOption<List<CustomerContactListResponse>> list(@Validated @RequestBody CustomerContactPageRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                        OrganizationContext.getOrganizationId(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/list/{customerId}")
    @Operation(summary = "客户下的联系人列表")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    public CustomerContactListAllResponse list(@Validated @PathVariable String customerId) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerContactService.listByCustomerId(customerId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ}, logical = Logical.OR)
    @Operation(summary = "客户联系人详情")
    public CustomerContactGetResponse get(@PathVariable String id){
        return customerContactService.get(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_ADD,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_ADD}, logical = Logical.OR)
    @Operation(summary = "添加客户联系人")
    public CustomerContact add(@Validated @RequestBody CustomerContactAddRequest request) {
		return customerContactService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE}, logical = Logical.OR)
    @Operation(summary = "更新客户联系人")
    public CustomerContact update(@Validated @RequestBody CustomerContactUpdateRequest request) {
        return customerContactService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/enable/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE}, logical = Logical.OR)
    @Operation(summary = "启用联系人")
    public void enable(@PathVariable String id){
        customerContactService.enable(id);
    }

    @PostMapping("/disable/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE}, logical = Logical.OR)
    @Operation(summary = "禁用联系人")
    public void disable(@PathVariable String id, @RequestBody CustomerContactDisableRequest request){
        customerContactService.disable(id, request);
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_DELETE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE}, logical = Logical.OR)
    @Operation(summary = "删除客户联系人")
    public void delete(@PathVariable String id) {
		customerContactService.delete(id);
    }

    @GetMapping("/opportunity/check/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_DELETE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE}, logical = Logical.OR)
    @Operation(summary = "检查客户联系人是否有关联商机")
    public boolean checkOpportunity(@PathVariable String id) {
        return customerContactService.checkOpportunity(id);
    }
}
