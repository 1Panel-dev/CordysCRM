package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.service.CustomerContactExportService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Tag(name = "客户联系人")
@RestController
@RequestMapping("/account/contact")
public class CustomerContactController {
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private CustomerContactExportService customerContactExportService;

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
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/chart")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人图表生成")
    public List<ChartResult> chart(@Validated @RequestBody ChartAnalysisRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactService.chart(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
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
    public CustomerContactGetResponse get(@PathVariable String id) {
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
    public void enable(@PathVariable String id) {
        customerContactService.enable(id);
    }

    @PostMapping("/disable/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
            PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE}, logical = Logical.OR)
    @Operation(summary = "禁用联系人")
    public void disable(@PathVariable String id, @RequestBody CustomerContactDisableRequest request) {
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

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "所有所有和部门联系人tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return customerContactService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/export-all")
    @Operation(summary = "联系人导出全部")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_EXPORT)
    public String customerContactExportAll(@Validated @RequestBody CustomerContactExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactExportService.export(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), deptDataPermission, LocaleContextHolder.getLocale());
    }


    @PostMapping("/export-select")
    @Operation(summary = "导出选中联系人")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_EXPORT)
    public String customerContactExportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return customerContactExportService.exportSelect(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/template/download")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        customerContactService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_IMPORT)
    public ImportResponse preCheck(@RequestPart(value = "file") MultipartFile file) {
        return customerContactService.importPreCheck(file, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_IMPORT)
    public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
        return customerContactService.realImport(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE)
    @Operation(summary = "批量更新客户联系人")
    public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request){
        customerContactService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
