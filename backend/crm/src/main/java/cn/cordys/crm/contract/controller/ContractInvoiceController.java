package cn.cordys.crm.contract.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.ContractInvoiceGetResponse;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import cn.cordys.crm.contract.service.ContractInvoiceExportService;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "发票")
@RestController
@RequestMapping("/invoice")
public class ContractInvoiceController {
  @Resource private ContractInvoiceService contractInvoiceService;
  @Resource private ContractInvoiceExportService contractInvoiceExportService;
  @Resource private DataScopeService dataScopeService;

  @GetMapping("/module/form")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
  @Operation(summary = "获取表单配置")
  public ModuleFormConfigDTO getModuleFormConfig() {
    return contractInvoiceService.getBusinessFormConfig(OrganizationContext.getOrganizationId());
  }

  @PostMapping("/page")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
  @Operation(summary = "列表")
  public PagerWithOption<List<ContractInvoiceListResponse>> list(
      @Validated @RequestBody ContractInvoicePageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_INVOICE_READ);
    return contractInvoiceService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @GetMapping("/get/snapshot/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
  @Operation(summary = "获取详情快照")
  public ContractInvoiceGetResponse getSnapshot(@PathVariable("id") String id) {
    return contractInvoiceService.getSnapshotWithDataPermissionCheck(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/get/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_READ)
  @Operation(summary = "详情")
  public ContractInvoiceGetResponse get(@PathVariable("id") String id) {
    return contractInvoiceService.getWithDataPermissionCheck(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_ADD)
  @Operation(summary = "创建")
  public ContractInvoice add(@Validated @RequestBody ContractInvoiceAddRequest request) {
    return contractInvoiceService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_UPDATE)
  @Operation(summary = "更新")
  public ContractInvoice update(@Validated @RequestBody ContractInvoiceUpdateRequest request) {
    return contractInvoiceService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_DELETE)
  @Operation(summary = "删除")
  public void delete(@PathVariable("id") String id) {
    contractInvoiceService.delete(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
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
    return contractInvoiceService.getTabEnableConfig(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/export-select")
  @Operation(summary = "导出选中发票")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_EXPORT)
  public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            PermissionConstants.CONTRACT_INVOICE_READ);
    ExportDTO exportDTO =
        ExportDTO.builder()
            .exportType(ExportConstants.ExportType.CONTRACT_INVOICE.name())
            .fileName(request.getFileName())
            .headList(request.getHeadList())
            .logModule(LogModule.CONTRACT_INVOICE)
            .locale(LocaleContextHolder.getLocale())
            .orgId(OrganizationContext.getOrganizationId())
            .userId(SessionUtils.getUserId())
            .deptDataPermission(deptDataPermission)
            .selectIds(request.getIds())
            .selectRequest(request)
            .formKey(FormKey.INVOICE.getKey())
            .build();
    return contractInvoiceExportService.exportSelect(exportDTO);
  }

  @PostMapping("/export-all")
  @Operation(summary = "导出全部合同")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_EXPORT)
  public String exportAll(@Validated @RequestBody ContractInvoiceExportRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_INVOICE_EXPORT);
    ExportDTO exportDTO =
        ExportDTO.builder()
            .exportType(ExportConstants.ExportType.CONTRACT_INVOICE.name())
            .fileName(request.getFileName())
            .headList(request.getHeadList())
            .logModule(LogModule.CONTRACT_INVOICE)
            .locale(LocaleContextHolder.getLocale())
            .orgId(OrganizationContext.getOrganizationId())
            .userId(SessionUtils.getUserId())
            .deptDataPermission(deptDataPermission)
            .pageRequest(request)
            .formKey(FormKey.INVOICE.getKey())
            .build();
    return contractInvoiceExportService.export(exportDTO);
  }

  @PostMapping("/batch/delete")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_DELETE)
  @Operation(summary = "批量删除客户")
  public void batchDelete(@RequestBody @NotNull List<String> ids) {
    contractInvoiceService.batchDelete(
        ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/approval")
  @RequiresPermissions(PermissionConstants.CONTRACT_INVOICE_APPROVAL)
  @Operation(summary = "审核通过/不通过")
  public void approval(@Validated @RequestBody ContractInvoiceApprovalRequest request) {
    contractInvoiceService.approvalContractInvoice(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/revoke/{id}")
  @Operation(summary = "撤销审批")
  public String revoke(@PathVariable("id") String id) {
    return contractInvoiceService.revoke(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }
}
