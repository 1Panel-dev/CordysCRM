package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.*;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.utils.ConditionFilterUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.service.CustomerExportService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import io.cordys.crm.system.dto.request.PoolReasonRequest;
import io.cordys.crm.system.dto.response.BatchAffectResponse;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "客户")
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Resource
    private CustomerService customerService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private CustomerExportService customerExportService;


    @GetMapping("/module/form")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户列表")
    public PagerWithOption<List<CustomerListResponse>> list(@Validated @RequestBody CustomerPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户详情")
    public CustomerGetResponse get(@PathVariable String id) {
        return customerService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)
    @Operation(summary = "添加客户")
    public Customer add(@Validated @RequestBody CustomerAddRequest request) {
        return customerService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "更新客户")
    public Customer update(@Validated @RequestBody CustomerUpdateRequest request) {
        return customerService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE)
    @Operation(summary = "删除客户")
    public void delete(@PathVariable String id) {
        customerService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/transfer")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "批量转移客户")
    public void batchTransfer(@RequestBody CustomerBatchTransferRequest request) {
        customerService.batchTransfer(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除客户")
    public void batchDelete(@RequestBody @NotNull List<String> ids) {
        customerService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/to-pool")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE)
    @Operation(summary = "批量移入公海")
    public BatchAffectResponse batchToPool(@RequestBody BatchPoolReasonRequest request) {
        return customerService.batchToPool(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/to-pool")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE)
    @Operation(summary = "移入公海")
    public BatchAffectResponse toPool(@Validated @RequestBody PoolReasonRequest request) {
        return customerService.toPool(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/option")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户选项")
    public Pager<List<OptionDTO>> getCustomerOptions(@Validated @RequestBody BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, customerService.getCustomerOptions(request.getKeyword(), OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "所有客户和部门客户tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return customerService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/opportunity/page")
    @RequiresPermissions({PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
    @Operation(summary = "客户详情-商机列表")
    public PagerWithOption<List<OpportunityListResponse>> list(@Validated @RequestBody CustomerOpportunityPageRequest request) {
        request.setViewId("ALL");
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/export-all")
    @Operation(summary = "客户导出全部")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT)
    public String opportunityExportAll(@Validated @RequestBody CustomerExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerExportService.export(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), deptDataPermission, LocaleContextHolder.getLocale());
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中客户")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT)
    public String opportunityExportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return customerExportService.exportSelect(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

}
