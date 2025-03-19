package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.follow.domain.FollowUpPlan;
import io.cordys.crm.follow.dto.CustomerDataDTO;
import io.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanDetailResponse;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.follow.service.FollowUpPlanService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户跟进计划")
@RestController
@RequestMapping("/customer/follow/plan")
public class CustomerFollowPlanController {

    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)
    @Operation(summary = "添加客户跟进计划")
    public FollowUpPlan add(@Validated @RequestBody FollowUpPlanAddRequest request) {
        return followUpPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "更新客户跟进计划")
    public FollowUpPlan update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户跟进计划列表")
    public Pager<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        CustomerDataDTO customerData = followUpPlanService.getCustomerPermission(SessionUtils.getUserId(), request.getSourceId());
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CUSTOMER", "CUSTOMER", customerData));
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户跟进计划详情")
    public FollowUpPlanDetailResponse get(@PathVariable String id) {
        return followUpPlanService.get(id);
    }


    @GetMapping("/cancel/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "取消客户跟进计划")
    public void cancelPlan(@PathVariable String id) {
        followUpPlanService.cancelPlan(id);
    }
}
