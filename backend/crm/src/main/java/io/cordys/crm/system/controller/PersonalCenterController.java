package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.response.ClueRepeatListResponse;
import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.follow.service.FollowUpPlanService;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.dto.request.PersonalInfoRequest;
import io.cordys.crm.system.dto.request.PersonalPasswordRequest;
import io.cordys.crm.system.dto.request.RepeatCustomerDetailPageRequest;
import io.cordys.crm.system.dto.request.RepeatCustomerPageRequest;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.crm.system.service.PersonalCenterService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Resource
    private ExtUserRoleMapper extUserRoleMapper;

    @Resource
    private BaseMapper<Module> moduleMapper;


    @PostMapping("/repeat/customer")
    @Operation(summary = "获取重复客户相关数据")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ}, logical = Logical.OR)
    public Pager<List<CustomerRepeatResponse>> getRepeatCustomer(@Validated @RequestBody RepeatCustomerPageRequest request) {
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(SessionUtils.getUserId());
        LambdaQueryWrapper<Module> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Module::getOrganizationId, OrganizationContext.getOrganizationId());
        queryWrapper.eq(Module::getEnable, true);
        List<Module> modules = moduleMapper.selectListByLambda(queryWrapper);
        List<String> keyList = modules.stream().map(Module::getModuleKey).toList();
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, personalCenterService.getRepeatCustomer(request,permissions,keyList,OrganizationContext.getOrganizationId(), SessionUtils.getUserId()));
    }


    @PostMapping("/repeat/clue")
    @Operation(summary = "获取重复线索相关数据")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ})
    public Pager<List<ClueRepeatListResponse>> getRepeatClue(@Validated @RequestBody RepeatCustomerPageRequest request) {
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(SessionUtils.getUserId());
        LambdaQueryWrapper<Module> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Module::getOrganizationId, OrganizationContext.getOrganizationId());
        queryWrapper.eq(Module::getEnable, true);
        queryWrapper.eq(Module::getModuleKey, ModuleKey.CLUE.getKey());
        List<Module> modules = moduleMapper.selectListByLambda(queryWrapper);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        //模块关闭，但是有权限，返回指定code
        if ((permissions.indexOf(PermissionConstants.CLUE_MANAGEMENT_READ) > 0 || StringUtils.equalsIgnoreCase(SessionUtils.getUserId(), InternalUser.ADMIN.getValue()) && CollectionUtils.isEmpty(modules))) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }else {
            return PageUtils.setPageInfo(page, personalCenterService.getRepeatClue(request,OrganizationContext.getOrganizationId()));
        }
    }


    @PostMapping("/repeat/clue/detail")
    @Operation(summary = "获取重复线索详情")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ})
    public List<ClueRepeatListResponse> getRepeatClueDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        return personalCenterService.getRepeatClueDetail(request,OrganizationContext.getOrganizationId());
    }
    
    @PostMapping("/repeat/opportunity/detail")
    @Operation(summary = "获取重复商机详情")
    @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
    public List<OpportunityRepeatResponse> getRepeatOpportunityDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        return personalCenterService.getRepeatOpportunityDetail(request);
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
