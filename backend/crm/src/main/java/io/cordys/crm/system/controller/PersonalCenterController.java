package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.response.ClueRepeatListResponse;
import io.cordys.crm.customer.dto.response.CustomerContactRepeatResponse;
import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.service.PersonalCenterService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/personal/center")
@Tag(name = "个人中心")
public class PersonalCenterController {
    @Resource
    private PersonalCenterService personalCenterService;


    @PostMapping("/repeat/customer")
    @Operation(summary = "获取重复客户相关数据")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ}, logical = Logical.OR)
    public Pager<List<CustomerRepeatResponse>> getRepeatCustomer(@Validated @RequestBody RepeatCustomerPageRequest request) {
        return personalCenterService.getRepeatCustomer(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/repeat/contact")
    @Operation(summary = "获取重复联系人相关数据")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ}, logical = Logical.OR)
    public Pager<List<CustomerContactRepeatResponse>> getRepeatCustomerContact(@Validated @RequestBody RepeatCustomerPageRequest request) {
        return personalCenterService.getRepeatCustomerContact(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/repeat/clue")
    @Operation(summary = "获取重复线索相关数据")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ})
    public Pager<List<ClueRepeatListResponse>> getRepeatClue(@Validated @RequestBody RepeatCustomerPageRequest request) {
        return personalCenterService.getRepeatClue(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/repeat/clue/detail")
    @Operation(summary = "获取重复线索详情")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ})
    public Pager<List<ClueRepeatListResponse>> getRepeatClueDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, personalCenterService.getRepeatClueDetail(request, OrganizationContext.getOrganizationId()));
    }

    @PostMapping("/repeat/opportunity/detail")
    @Operation(summary = "获取重复商机详情")
    @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
    public Pager<List<OpportunityRepeatResponse>> getRepeatOpportunityDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, personalCenterService.getRepeatOpportunityDetail(request));
    }


    @PostMapping("/follow/plan/list")
    @Operation(summary = "用户跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        request.setMyPlan(true);
        return personalCenterService.getPlanList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/info")
    @Operation(summary = "当前用户详情")
    public UserResponse getUserDetail() {
        return personalCenterService.getUserDetail(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新户详情")
    public UserResponse updateInfo(@Validated @RequestBody PersonalInfoRequest personalInfoRequest) {
        return personalCenterService.updateInfo(personalInfoRequest, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    /**
     * 发送验证码
     */
    @PostMapping("/mail/code/send")
    public void sendCode(@RequestBody @NotNull SendEmailDTO email) {
        personalCenterService.sendCode(email, OrganizationContext.getOrganizationId());
    }

    /**
     * 更新密码
     */
    @PostMapping("/info/reset")
    @Operation(summary = "用户密码重置")
    public void resetUserPassword(@Validated @RequestBody PersonalPasswordRequest personalPasswordRequest) {
        personalCenterService.resetUserPassword(personalPasswordRequest, SessionUtils.getUserId());
    }
}
