package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationBatchRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationEditRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.BatchAffectSkipResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商机报价单")
@RestController
@RequestMapping("/opportunity/quotation")
public class OpportunityQuotationController {

  @Resource private OpportunityQuotationService opportunityQuotationService;
  @Resource private ModuleFormCacheService moduleFormCacheService;
  @Resource private DataScopeService dataScopeService;

  @GetMapping("/module/form")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "获取表单配置")
  public ModuleFormConfigDTO getModuleFormConfig() {
    return moduleFormCacheService.getBusinessFormConfig(
        FormKey.QUOTATION.getKey(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/page")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "报价单列表")
  public PagerWithOption<List<OpportunityQuotationListResponse>> list(
      @Validated @RequestBody OpportunityQuotationPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.OPPORTUNITY_QUOTATION_READ);
    return opportunityQuotationService.list(
        request,
        OrganizationContext.getOrganizationId(),
        SessionUtils.getUserId(),
        deptDataPermission,
        false);
  }

  // 新增
  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_ADD)
  @Operation(summary = "新增报价单")
  public OpportunityQuotation add(@Validated @RequestBody OpportunityQuotationAddRequest request) {
    return opportunityQuotationService.add(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  // 编辑
  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_UPDATE)
  @Operation(summary = "更新报价单")
  public OpportunityQuotation update(
      @Validated @RequestBody OpportunityQuotationEditRequest request) {
    return opportunityQuotationService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  // 查询详情
  @GetMapping("/get/snapshot/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "获取报价单快照详情")
  public OpportunityQuotationGetResponse getSnapshot(@PathVariable("id") String id) {
    return opportunityQuotationService.getSnapshot(id);
  }

  // 查询详情
  @GetMapping("/get/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "获取报价单详情")
  public OpportunityQuotationGetResponse get(@PathVariable("id") String id) {
    return opportunityQuotationService.get(id);
  }

  @GetMapping("/module/form/snapshot/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "获取表单快照配置")
  public ModuleFormConfigDTO getFormSnapshot(@PathVariable("id") String id) {
    return opportunityQuotationService.getFormSnapshot(id, OrganizationContext.getOrganizationId());
  }

  // 撤销审批
  @GetMapping("/revoke/{id}")
  @Operation(summary = "撤销报价单审批")
  public String revoke(@PathVariable("id") String id) {
    return opportunityQuotationService.revoke(id, SessionUtils.getUserId());
  }

  // 作废
  @GetMapping("/voided/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_VOIDED)
  @Operation(summary = "作废报价单")
  public String voidQuotation(@PathVariable("id") String id) {
    return opportunityQuotationService.voidQuotation(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  // 批量作废报价单
  @PostMapping("/batch/voided")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_VOIDED)
  @Operation(summary = "批量作废报价单")
  public BatchAffectReasonResponse batchVoidQuotation(
      @Validated @RequestBody OpportunityQuotationBatchRequest request) {
    return opportunityQuotationService.batchVoidQuotation(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  // 审批
  @PostMapping("/approve")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_APPROVAL)
  @Operation(summary = "审批报价单")
  public String approve(@RequestBody OpportunityQuotationEditRequest request) {
    return opportunityQuotationService.approve(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  // 批量审批
  @PostMapping("/batch/approve")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_APPROVAL)
  @Operation(summary = "批量审批报价单")
  public BatchAffectSkipResponse batchApprove(
      @Validated @RequestBody OpportunityQuotationBatchRequest request) {
    return opportunityQuotationService.batchApprove(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  // 删除报价单
  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_DELETE)
  @Operation(summary = "删除报价单")
  public void delete(@PathVariable("id") String id) {
    opportunityQuotationService.delete(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/tab")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
  @Operation(summary = "所有商机报价单和部门商机报价单tab是否显示")
  public ResourceTabEnableDTO getTabEnableConfig() {
    return opportunityQuotationService.getTabEnableConfig(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/download/{id}")
  @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_DOWNLOAD)
  @Operation(summary = "下载报价单日志记录")
  public void download(@PathVariable("id") String id) {
    opportunityQuotationService.download(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }
}
