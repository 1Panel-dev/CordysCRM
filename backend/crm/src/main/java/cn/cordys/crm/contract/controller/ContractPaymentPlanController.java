package cn.cordys.crm.contract.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;

import cn.cordys.common.pager.Pager;
import cn.cordys.security.SessionUtils;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.*;

import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.common.pager.PageUtils;
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

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CONTRACT_CONTRACT_PAYMENT_PLAN_READ)
    @Operation(summary = "合同回款计划列表")
    public Pager<List<ContractPaymentPlanListResponse>> list(@Validated @RequestBody ContractPaymentPlanPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, contractPaymentPlanService.list(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_CONTRACT_PAYMENT_PLAN_READ)
    @Operation(summary = "合同回款计划详情")
    public ContractPaymentPlanGetResponse get(@PathVariable String id){
        return contractPaymentPlanService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_CONTRACT_PAYMENT_PLAN_ADD)
    @Operation(summary = "添加合同回款计划")
    public ContractPaymentPlan add(@Validated @RequestBody ContractPaymentPlanAddRequest request) {
		return contractPaymentPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_CONTRACT_PAYMENT_PLAN_UPDATE)
    @Operation(summary = "更新合同回款计划")
    public ContractPaymentPlan update(@Validated @RequestBody ContractPaymentPlanUpdateRequest request) {
        return contractPaymentPlanService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_CONTRACT_PAYMENT_PLAN_DELETE)
    @Operation(summary = "删除合同回款计划")
    public void delete(@PathVariable String id) {
		contractPaymentPlanService.delete(id);
    }
}
