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
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanExportRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.service.ContractPaymentPlanExportService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Tag(name = "合同回款计划")
@RestController
@RequestMapping("/contract/payment-plan")
public class ContractPaymentPlanController {
  @Resource private ContractPaymentPlanService contractPaymentPlanService;
  @Resource private ContractPaymentPlanExportService contractPaymentPlanExportService;
  @Resource private DataScopeService dataScopeService;
  @Resource private ModuleFormCacheService moduleFormCacheService;

  @GetMapping("/module/form")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  @Operation(summary = "获取表单配置")
  public ModuleFormConfigDTO getModuleFormConfig() {
    return moduleFormCacheService.getBusinessFormConfig(
        FormKey.CONTRACT_PAYMENT_PLAN.getKey(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/page")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  @Operation(summary = "合同回款计划列表")
  public PagerWithOption<List<ContractPaymentPlanListResponse>> list(
      @Validated @RequestBody ContractPaymentPlanPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
    return contractPaymentPlanService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @GetMapping("/get/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  @Operation(summary = "合同回款计划详情")
  public ContractPaymentPlanGetResponse get(@PathVariable String id) {
    return contractPaymentPlanService.getWithDataPermissionCheck(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_ADD)
  @Operation(summary = "添加合同回款计划")
  public ContractPaymentPlan add(@Validated @RequestBody ContractPaymentPlanAddRequest request) {
    return contractPaymentPlanService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_UPDATE)
  @Operation(summary = "更新合同回款计划")
  public ContractPaymentPlan update(
      @Validated @RequestBody ContractPaymentPlanUpdateRequest request) {
    return contractPaymentPlanService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_DELETE)
  @Operation(summary = "删除合同回款计划")
  public void delete(@PathVariable String id) {
    contractPaymentPlanService.delete(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/tab")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  @Operation(summary = "tab是否显示")
  public ResourceTabEnableDTO getTabEnableConfig() {
    return contractPaymentPlanService.getTabEnableConfig(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/export-select")
  @Operation(summary = "导出选中回款计划")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
    ExportDTO exportDTO =
        ExportDTO.builder()
            .exportType(ExportConstants.ExportType.CONTRACT_PAYMENT_PLAN.name())
            .fileName(request.getFileName())
            .headList(request.getHeadList())
            .logModule(LogModule.CONTRACT_PAYMENT)
            .locale(LocaleContextHolder.getLocale())
            .orgId(OrganizationContext.getOrganizationId())
            .userId(SessionUtils.getUserId())
            .deptDataPermission(deptDataPermission)
            .selectIds(request.getIds())
            .selectRequest(request)
            .build();
    return contractPaymentPlanExportService.exportSelect(exportDTO);
  }

  @PostMapping("/export-all")
  @Operation(summary = "导出全部回款计划")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
  public String exportAll(@Validated @RequestBody ContractPaymentPlanExportRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
    ExportDTO exportDTO =
        ExportDTO.builder()
            .exportType(ExportConstants.ExportType.CONTRACT_PAYMENT_PLAN.name())
            .fileName(request.getFileName())
            .headList(request.getHeadList())
            .logModule(LogModule.CONTRACT_PAYMENT)
            .locale(LocaleContextHolder.getLocale())
            .orgId(OrganizationContext.getOrganizationId())
            .userId(SessionUtils.getUserId())
            .deptDataPermission(deptDataPermission)
            .pageRequest(request)
            .build();
    return contractPaymentPlanExportService.export(exportDTO);
  }
}
