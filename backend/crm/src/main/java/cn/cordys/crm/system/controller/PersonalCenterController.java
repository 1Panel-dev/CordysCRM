package cn.cordys.crm.system.controller;

import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.system.dto.request.PersonalInfoRequest;
import cn.cordys.crm.system.dto.request.PersonalPasswordRequest;
import cn.cordys.crm.system.dto.request.SendEmailDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.service.PersonalCenterService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/personal/center")
@Tag(name = "个人中心")
public class PersonalCenterController {
    @Resource
    private PersonalCenterService personalCenterService;


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


    @PostMapping("/follow/plan/list")
    @Operation(summary = "用户跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        request.setMyPlan(true);
        return personalCenterService.getPlanList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
