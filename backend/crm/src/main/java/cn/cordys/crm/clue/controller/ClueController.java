package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ChartAnalysisRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.*;
import cn.cordys.crm.clue.dto.response.ClueGetResponse;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.service.ClueExportService;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.dto.request.BatchReTransitionCustomerRequest;
import cn.cordys.crm.customer.dto.request.ClueTransformRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "线索")
@RequestMapping("/lead")
public class ClueController {

    @Resource
    private ClueService clueService;
    @Resource
    private ClueExportService clueExportService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private CustomerService customerService;

    @GetMapping("/module/form")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索列表")
    public PagerWithOption<List<ClueListResponse>> list(@Validated @RequestBody CluePageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return clueService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索详情")
    public ClueGetResponse get(@PathVariable String id) {
        return clueService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_ADD)
    @Operation(summary = "添加线索")
    public Clue add(@Validated @RequestBody ClueAddRequest request) {
        return clueService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索")
    public Clue update(@Validated @RequestBody ClueUpdateRequest request) {
        return clueService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/status/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索状态")
    public void updateStatus(@Validated @RequestBody ClueStatusUpdateRequest request) {
        clueService.updateStatus(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_DELETE)
    @Operation(summary = "删除线索")
    public void delete(@PathVariable String id) {
        clueService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/transition/account")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)
    @Operation(summary = "转为客户")
    public void transitionCustomer(@Validated @RequestBody ClueTransitionCustomerRequest request) {
        clueService.transitionCustomer(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/transfer")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "批量转移线索")
    public void batchTransfer(@RequestBody ClueBatchTransferRequest request) {
        clueService.batchTransfer(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "批量更新线索")
    public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
        clueService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除线索")
    public void batchDelete(@RequestBody @NotNull List<String> ids) {
        clueService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/to-pool")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_RECYCLE)
    @Operation(summary = "批量移入线索池")
    public BatchAffectResponse batchToPool(@RequestBody BatchPoolReasonRequest request) {
        return clueService.batchToPool(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/to-pool")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE)
    @Operation(summary = "移入线索池")
    public BatchAffectResponse toPool(@Validated @RequestBody PoolReasonRequest request) {
        return clueService.toPool(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "所有线索和部门线索tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return clueService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/export")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_EXPORT)
    @Operation(summary = "导出全部")
    public String exportAll(@Validated @RequestBody ClueExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return clueExportService.exportAll(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, LocaleContextHolder.getLocale());
    }

    @PostMapping("/export-select")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_EXPORT)
    @Operation(summary = "导出选中")
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return clueExportService.exportSelect(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @PostMapping("/transition/account/page")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.AND)
    @Operation(summary = "客户转移分页查询")
    public PagerWithOption<List<CustomerListResponse>> transitionCustomerPage(@Validated @RequestBody CustomerPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        return customerService.transitionList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/re-transition/account")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "批量关联已有客户")
    public void batchTransition(@Validated @RequestBody BatchReTransitionCustomerRequest request) {
        clueService.batchTransition(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/transform")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "转换")
    public String transform(@Validated @RequestBody ClueTransformRequest request) {
        return clueService.transform(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/template/download")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        clueService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_IMPORT)
    public ImportResponse preCheck(@RequestPart(value = "file") MultipartFile file) {
        return clueService.importPreCheck(file, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_IMPORT)
    public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
        return clueService.realImport(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/chart")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "客户图表生成")
    public List<ChartResult> chart(@Validated @RequestBody ChartAnalysisRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return clueService.chart(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }
}
