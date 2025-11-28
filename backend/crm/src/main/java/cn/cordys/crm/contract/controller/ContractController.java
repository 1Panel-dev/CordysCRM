package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.ContractDetailPaymentPlanPageRequest;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.ContractResponse;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractService;
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


@Tag(name = "合同")
@RestController
@RequestMapping("/contract")
public class ContractController {
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ContractService contractService;
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
        return contractService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }


    @PostMapping("/voided/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_VOIDED)
    @Operation(summary = "作废")
    public void voided(@Validated @RequestBody ContractVoidRequest request) {
        contractService.voidContract(request, SessionUtils.getUserId());
    }


    @PostMapping("/archived")
    @RequiresPermissions(PermissionConstants.CONTRACT_ARCHIVE)
    @Operation(summary = "归档/取消归档")
    public void archived(@Validated @RequestBody ContractArchivedRequest request) {
        contractService.archivedContract(request, SessionUtils.getUserId());
    }


    @PostMapping("/{id}/payment-plan/page")
    @RequiresPermissions({PermissionConstants.CONTRACT_READ, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ})
    @Operation(summary = "合同详情-回款列表")
    public PagerWithOption<List<ContractPaymentPlanListResponse>> paymentPlanList(@PathVariable("id") String id,
                                                                                  @Validated @RequestBody ContractPaymentPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        request.setViewId(InternalUserView.ALL.name());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        ContractDetailPaymentPlanPageRequest contractPaymentPlanPageRequest = BeanUtils.copyBean(new ContractDetailPaymentPlanPageRequest(), request);
        contractPaymentPlanPageRequest.setContractId(id);
        return contractPaymentPlanService.list(contractPaymentPlanPageRequest, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }


    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_QUOTATION_READ)
    @Operation(summary = "tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return contractService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
