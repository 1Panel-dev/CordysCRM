package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.follow.service.FollowUpPlanService;
import io.cordys.crm.system.dto.request.PersonalInfoRequest;
import io.cordys.crm.system.dto.request.PersonalPasswordRequest;
import io.cordys.crm.system.dto.response.RepeatCustomerResponse;
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

    @Resource
    private FollowUpPlanService followUpPlanService;


    @GetMapping("/repeat/customer")
    @Operation(summary = "获取重复客户相关数据")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ}, logical = Logical.OR)
    public RepeatCustomerResponse getRepeatCustomer(@RequestParam(value = "name") String name) {
        return personalCenterService.getRepeatCustomer(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), name);
    }


    @PostMapping("/follow/plan/list")
    @Operation(summary = "用户跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        request.setMyPlan(true);
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "NULL", null, null);
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
    public void sendCode(@RequestBody @NotNull String email) {
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
