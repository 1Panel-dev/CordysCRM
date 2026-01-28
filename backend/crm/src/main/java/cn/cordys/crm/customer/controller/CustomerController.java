package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.*;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerExportService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "客户")
@RestController
@RequestMapping("/account")
public class CustomerController {

  @Resource private CustomerService customerService;
  @Resource private OpportunityService opportunityService;
  @Resource private ModuleFormCacheService moduleFormCacheService;
  @Resource private DataScopeService dataScopeService;
  @Resource private CustomerExportService customerExportService;
  @Resource private ContractService contractService;
  @Resource private ContractPaymentPlanService contractPaymentPlanService;
  @Resource private ContractPaymentRecordService contractPaymentRecordService;
  @Resource private ContractInvoiceService contractInvoiceService;

  @GetMapping("/module/form")
  @RequiresPermissions(
      value = {
        PermissionConstants.CUSTOMER_MANAGEMENT_READ,
        PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ
      },
      logical = Logical.OR)
  @Operation(summary = "获取表单配置")
  public ModuleFormConfigDTO getModuleFormConfig() {
    return moduleFormCacheService.getBusinessFormConfig(
        FormKey.CUSTOMER.getKey(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/page")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "客户列表")
  public PagerWithOption<List<CustomerListResponse>> list(
      @Validated @RequestBody CustomerPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CUSTOMER_MANAGEMENT_READ);
    return customerService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @GetMapping("/get/{id}")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "客户详情")
  public CustomerGetResponse get(@PathVariable String id) {
    return customerService.getWithDataPermissionCheck(
        id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)
  @Operation(summary = "添加客户")
  public Customer add(@Validated @RequestBody CustomerAddRequest request) {
    return customerService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  @Operation(summary = "更新客户")
  public Customer update(@Validated @RequestBody CustomerUpdateRequest request) {
    return customerService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE)
  @Operation(summary = "删除客户")
  public void delete(@PathVariable String id) {
    customerService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/batch/transfer")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_TRANSFER)
  @Operation(summary = "批量转移客户")
  public void batchTransfer(@RequestBody CustomerBatchTransferRequest request) {
    customerService.batchTransfer(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/batch/update")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  @Operation(summary = "批量更新客户")
  public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
    customerService.batchUpdate(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/batch/delete")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE)
  @Operation(summary = "批量删除客户")
  public void batchDelete(@RequestBody @NotNull List<String> ids) {
    customerService.batchDelete(
        ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/batch/to-pool")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE)
  @Operation(summary = "批量移入公海")
  public BatchAffectResponse batchToPool(@RequestBody BatchPoolReasonRequest request) {
    return customerService.batchToPool(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/to-pool")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE)
  @Operation(summary = "移入公海")
  public BatchAffectResponse toPool(@Validated @RequestBody PoolReasonRequest request) {
    return customerService.toPool(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/option")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "客户选项")
  public Pager<List<OptionDTO>> getCustomerOptions(
      @Validated @RequestBody BasePageRequest request) {
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    return PageUtils.setPageInfo(
        page,
        customerService.getCustomerOptions(
            request.getKeyword(), OrganizationContext.getOrganizationId()));
  }

  @GetMapping("/tab")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "所有客户和部门客户tab是否显示")
  public ResourceTabEnableDTO getTabEnableConfig() {
    return customerService.getTabEnableConfig(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/opportunity/page")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.OPPORTUNITY_MANAGEMENT_READ
  })
  @Operation(summary = "客户详情-商机列表")
  public PagerWithOption<List<OpportunityListResponse>> list(
      @Validated @RequestBody CustomerOpportunityPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    request.setViewId("ALL");
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
    return opportunityService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission,
        false);
  }

  @PostMapping("/export-all")
  @Operation(summary = "客户导出全部")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT)
  public String opportunityExportAll(@Validated @RequestBody CustomerExportRequest request) {
    ConditionFilterUtils.parseCondition(request);
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CUSTOMER_MANAGEMENT_READ);
    return customerExportService.export(
        SessionUtils.getUserId(),
        request,
        OrganizationContext.getOrganizationId(),
        deptDataPermission,
        LocaleContextHolder.getLocale());
  }

  @PostMapping("/export-select")
  @Operation(summary = "导出选中客户")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_EXPORT)
  public String opportunityExportSelect(@Validated @RequestBody ExportSelectRequest request) {
    return customerExportService.exportSelect(
        SessionUtils.getUserId(),
        request,
        OrganizationContext.getOrganizationId(),
        LocaleContextHolder.getLocale());
  }

  @GetMapping("/template/download")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_IMPORT)
  @Operation(summary = "下载导入模板")
  public void downloadImportTpl(HttpServletResponse response) {
    customerService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
  }

  @PostMapping("/import/pre-check")
  @Operation(summary = "导入检查")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_IMPORT)
  public ImportResponse preCheck(@RequestPart(value = "file") MultipartFile file) {
    return customerService.importPreCheck(file, OrganizationContext.getOrganizationId());
  }

  @PostMapping("/import")
  @Operation(summary = "导入")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_IMPORT)
  public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
    return customerService.realImport(
        file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/merge/page")
  @Operation(summary = "分页获取合并客户列表")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  public Pager<List<CustomerListResponse>> sourceCustomerPage(
      @Valid @RequestBody CustomerPageRequest request) {
    request.setCombineSearch(request.getCombineSearch().convert());
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            "ALL",
            PermissionConstants.CUSTOMER_MANAGEMENT_READ);
    return customerService.sourceList(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @PostMapping("/merge")
  @Operation(summary = "合并客户")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_MERGE)
  public void merge(@Valid @RequestBody CustomerMergeRequest request) {
    customerService.merge(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/chart")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "客户图表生成")
  public List<ChartResult> chart(@Validated @RequestBody ChartAnalysisRequest request) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CUSTOMER_MANAGEMENT_READ);
    return customerService.chart(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @PostMapping("/contract/page")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_READ
  })
  @Operation(summary = "客户详情-合同列表")
  public PagerWithOption<List<ContractListResponse>> contractList(
      @Validated @RequestBody CustomerContractPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    request.setViewId(InternalUserView.ALL.name());
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_READ);
    return contractService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission,
        false);
  }

  @GetMapping("/contract/statistic/{accountId}")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_READ
  })
  @Operation(summary = "客户详情-合同列表统计")
  public CustomerContractStatisticResponse calculateCustomerContractStatistic(
      @PathVariable String accountId) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            PermissionConstants.CONTRACT_READ);
    return contractService.calculateContractStatisticByCustomerId(
        accountId,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @PostMapping("/contract/payment-plan/page")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_PAYMENT_PLAN_READ
  })
  @Operation(summary = "客户详情-合同回款计划列表")
  public PagerWithOption<List<ContractPaymentPlanListResponse>> contractList(
      @Validated @RequestBody CustomerContractPaymentPlanPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    request.setViewId(InternalUserView.ALL.name());
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

  @GetMapping("/contract/payment-plan/statistic/{accountId}")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_PAYMENT_PLAN_READ
  })
  @Operation(summary = "客户详情-合同回款计划列表统计")
  public CustomerPaymentPlanStatisticResponse calculateCustomerPaymentPlanStatistic(
      @PathVariable String accountId) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
    return contractPaymentPlanService.calculateCustomerPaymentPlanStatistic(
        accountId,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @PostMapping("/contract/payment-record/page")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_PAYMENT_RECORD_READ
  })
  @Operation(summary = "客户详情-合同回款记录列表")
  public PagerWithOption<List<ContractPaymentRecordResponse>> recordList(
      @Validated @RequestBody ContractPaymentRecordPageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    request.setViewId(InternalUserView.ALL.name());
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            request.getViewId(),
            PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
    return contractPaymentRecordService.list(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @GetMapping("/contract/payment-record/statistic/{accountId}")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_PAYMENT_RECORD_READ
  })
  @Operation(summary = "客户详情-合同回款记录列表统计")
  public CustomerPaymentRecordStatisticResponse calculateCustomerPaymentRecordStatistic(
      @PathVariable String accountId) {
    DeptDataPermissionDTO deptDataPermission =
        dataScopeService.getDeptDataPermission(
            SessionUtils.getUserId(),
            OrganizationContext.getOrganizationId(),
            PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
    return contractPaymentRecordService.sumCustomerPaymentAmount(
        accountId,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        deptDataPermission);
  }

  @PostMapping("/invoice/page")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_INVOICE_READ
  })
  @Operation(summary = "客户详情-发票列表")
  public PagerWithOption<List<ContractInvoiceListResponse>> invoiceList(
      @Validated @RequestBody CustomerContractInvoicePageRequest request) {
    ConditionFilterUtils.parseCondition(request);
    request.setViewId(InternalUserView.ALL.name());
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

  @GetMapping("/invoice/statistic/{accountId}")
  @RequiresPermissions({
    PermissionConstants.CUSTOMER_MANAGEMENT_READ,
    PermissionConstants.CONTRACT_INVOICE_READ
  })
  @Operation(summary = "客户详情-发票列表统计")
  public CustomerInvoiceStatisticResponse calculateCustomerInvoiceStatistic(
      @PathVariable String accountId) {
    BigDecimal invoiceAmount =
        contractInvoiceService.calculateCustomerInvoiceAmount(
            accountId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());

    CustomerInvoiceStatisticResponse response = new CustomerInvoiceStatisticResponse();
    response.setContractAmount(calculateCustomerContractStatistic(accountId).getTotalAmount());
    response.setInvoicedAmount(invoiceAmount);
    response.setUninvoicedAmount(response.getContractAmount().subtract(invoiceAmount));
    return response;
  }
}
