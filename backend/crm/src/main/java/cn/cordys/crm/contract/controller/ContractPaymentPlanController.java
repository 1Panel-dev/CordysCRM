package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Tag(name = "合同回款计划")
@RestController
@RequestMapping("/contract/payment-plan")
public class ContractPaymentPlanController {
    @Resource
    private ContractPaymentPlanService contractPaymentPlanService;
    @Resource
    private DataScopeService dataScopeService;

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
    @Operation(summary = "合同回款计划列表")
    public PagerWithOption<List<ContractPaymentPlanListResponse>> list(@Validated @RequestBody ContractPaymentPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        return contractPaymentPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)
    @Operation(summary = "合同回款计划详情")
    public ContractPaymentPlanGetResponse get(@PathVariable String id){
        return contractPaymentPlanService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_ADD)
    @Operation(summary = "添加合同回款计划")
    public ContractPaymentPlan add(@Validated @RequestBody ContractPaymentPlanAddRequest request) {
		return contractPaymentPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_UPDATE)
    @Operation(summary = "更新合同回款计划")
    public ContractPaymentPlan update(@Validated @RequestBody ContractPaymentPlanUpdateRequest request) {
        return contractPaymentPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_PLAN_DELETE)
    @Operation(summary = "删除合同回款计划")
    public void delete(@PathVariable String id) {
		contractPaymentPlanService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
