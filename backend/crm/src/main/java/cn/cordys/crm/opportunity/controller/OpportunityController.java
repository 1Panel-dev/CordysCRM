package cn.cordys.crm.opportunity.controller;

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
import cn.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.*;
import cn.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunitySearchStatisticResponse;
import cn.cordys.crm.opportunity.service.OpportunityExportService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "商机")
@RestController
@RequestMapping("/opportunity")
public class OpportunityController {

    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private OpportunityExportService opportunityExportService;


    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机列表")
    public PagerWithOption<List<OpportunityListResponse>> list(@Validated @RequestBody OpportunityPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, false);
    }

    @PostMapping("/statistic")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机统计")
    public OpportunitySearchStatisticResponse searchStatistic(@Validated @RequestBody OpportunitySearchStatisticRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.searchStatistic(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)
    @Operation(summary = "添加商机")
    public Opportunity add(@Validated @RequestBody OpportunityAddRequest request) {
        return opportunityService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "更新商机")
    public Opportunity update(@Validated @RequestBody OpportunityUpdateRequest request) {
        return opportunityService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "删除商机")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE)
    public void deleteOpportunity(@PathVariable String id) {
        opportunityService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/batch/transfer")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_TRANSFER)
    @Operation(summary = "批量转移商机")
    public void batchTransfer(@RequestBody OpportunityTransferRequest request) {
        opportunityService.transfer(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除商机")
    public void delete(@RequestBody @NotEmpty List<String> ids) {
        opportunityService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机详情")
    public OpportunityDetailResponse get(@PathVariable String id) {
        return opportunityService.get(id);
    }

    @PostMapping("/update/stage")
    @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, PermissionConstants.OPPORTUNITY_MANAGEMENT_RESIGN}, logical = Logical.OR)
    @Operation(summary = "更新商机阶段")
    public void updateStage(@RequestBody OpportunityStageRequest request) {
        opportunityService.updateStage(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "批量更新商机")
    public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
        opportunityService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "所有商机和部门商机tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return opportunityService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/contact/list/{opportunityId}")
    @Operation(summary = "商机下的联系人列表")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    public CustomerContactListAllResponse list(@Validated @PathVariable String opportunityId) {
        return opportunityService.getContactList(opportunityId, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/export-all")
    @Operation(summary = "商机导出全部")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_EXPORT)
    public String opportunityExportAll(@Validated @RequestBody OpportunityExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityExportService.export(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), deptDataPermission, LocaleContextHolder.getLocale());
    }


    @PostMapping("/export-select")
    @Operation(summary = "导出选中商机")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_EXPORT)
    public String opportunityExportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return opportunityExportService.exportSelect(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/template/download")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        opportunityService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_IMPORT)
    public ImportResponse preCheck(@RequestPart(value = "file") MultipartFile file) {
        return opportunityService.importPreCheck(file, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_IMPORT)
    public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
        return opportunityService.realImport(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/sort")
    @Operation(summary = "商机阶段看板拖拽排序")
    public void sortModule(@Validated @RequestBody OpportunitySortRequest request) {
        opportunityService.sort(request, SessionUtils.getUserId());
    }

    @PostMapping("/chart")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "客户图表生成")
    public List<ChartResult> chart(@Validated @RequestBody ChartAnalysisRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.chart(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }
}
