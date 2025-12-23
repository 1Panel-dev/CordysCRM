package cn.cordys.crm.contract.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.ContractDetailPaymentPlanPageRequest;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.ContractResponse;
import cn.cordys.crm.contract.service.ContractExportService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.response.BatchAffectSkipResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "合同")
@RestController
@RequestMapping("/contract")
public class ContractController {
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ContractService contractService;
    @Resource
    private ContractExportService contractExportService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ContractPaymentPlanService contractPaymentPlanService;


    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_ADD)
    @Operation(summary = "创建")
    public Contract add(@Validated @RequestBody ContractAddRequest request) {
        return contractService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_UPDATE)
    @Operation(summary = "更新")
    public Contract update(@Validated @RequestBody ContractUpdateRequest request) {
        return contractService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/stage/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_STAGE)
    @Operation(summary = "更新合同阶段")
    public void updateStage(@Validated @RequestBody ContractStageRequest request) {
        contractService.updateStage(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        contractService.delete(id);
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "详情")
    public ContractResponse get(@PathVariable("id") String id) {
        return contractService.get(id);
    }


    @GetMapping("/module/form/snapshot/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "获取表单快照配置")
    public ModuleFormConfigDTO getFormSnapshot(@PathVariable("id") String id) {
        return contractService.getFormSnapshot(id, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "列表")
    public PagerWithOption<List<ContractListResponse>> list(@Validated @RequestBody ContractPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_READ);
        return contractService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, false);
    }


    @PostMapping("/approval")
    @RequiresPermissions(PermissionConstants.CONTRACT_APPROVAL)
    @Operation(summary = "审核通过/不通过")
    public void approval(@Validated @RequestBody ContractApprovalRequest request) {
        contractService.approvalContract(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/revoke/{id}")
    @Operation(summary = "撤销审批")
    public String revoke(@PathVariable("id") String id) {
        return contractService.revoke(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/approval")
    @RequiresPermissions(PermissionConstants.CONTRACT_APPROVAL)
    @Operation(summary = "批量审核通过/不通过")
    public BatchAffectSkipResponse batchApproval(@Validated @RequestBody ContractApprovalBatchRequest request) {
        return contractService.batchApprovalContract(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/contract-payment-plan/page")
    @RequiresPermissions({PermissionConstants.CONTRACT_READ, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ})
    @Operation(summary = "合同详情-回款列表")
    public PagerWithOption<List<ContractPaymentPlanListResponse>> paymentPlanList(@Validated @RequestBody ContractDetailPaymentPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        request.setViewId(InternalUserView.ALL.name());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        return contractPaymentPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return contractService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中合同")
    @RequiresPermissions(PermissionConstants.CONTRACT_EXPORT)
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), PermissionConstants.CONTRACT_READ);
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CONTRACT.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CONTRACT_INDEX)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .deptDataPermission(deptDataPermission)
                .selectIds(request.getIds())
                .selectRequest(request)
                .formKey(FormKey.CONTRACT.getKey())
                .build();
        return contractExportService.exportSelectWithMergeStrategy(exportDTO);
    }

    @PostMapping("/export-all")
    @Operation(summary = "导出全部合同")
    @RequiresPermissions(PermissionConstants.CONTRACT_EXPORT)
    public String exportAll(@Validated @RequestBody ContractExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_READ);
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CONTRACT.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CONTRACT_INDEX)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .deptDataPermission(deptDataPermission)
                .pageRequest(request)
                .formKey(FormKey.CONTRACT.getKey())
                .build();
        return contractExportService.exportAllWithMergeStrategy(exportDTO);
    }
}
