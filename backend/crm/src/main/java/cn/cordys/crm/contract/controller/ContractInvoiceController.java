package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.contract.dto.request.ContractInvoiceAddRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoiceUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractInvoiceGetResponse;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "发票")
@RestController
@RequestMapping("/invoice")
public class ContractInvoiceController {
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ContractInvoiceService contractInvoiceService;
    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
    @Operation(summary = "列表")
    public PagerWithOption<List<ContractInvoiceListResponse>> list(@Validated @RequestBody ContractInvoicePageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_INVOICE_READ);
        return contractInvoiceService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
    @Operation(summary = "详情")
    public ContractInvoiceGetResponse get(@PathVariable("id") String id) {
        return contractInvoiceService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_ADD)
    @Operation(summary = "创建")
    public ContractInvoice add(@Validated @RequestBody ContractInvoiceAddRequest request) {
        return contractInvoiceService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_UPDATE)
    @Operation(summary = "更新")
    public ContractInvoice update(@Validated @RequestBody ContractInvoiceUpdateRequest request) {
        return contractInvoiceService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        contractInvoiceService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/module/form/snapshot/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
    @Operation(summary = "获取表单快照配置")
    public ModuleFormConfigDTO getFormSnapshot(@PathVariable("id") String id) {
        return contractInvoiceService.getFormSnapshot(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
    @Operation(summary = "tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return contractInvoiceService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
