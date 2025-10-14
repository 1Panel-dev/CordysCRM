package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BusinessDataPermission;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "跟进计划统一页面")
@RestController
@RequestMapping("/follow/plan")
public class FollowUpPlanController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private DataScopeService dataScopeService;


    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_PLAN.getKey(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "数据权限TAB")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return followUpPlanService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "跟进记录列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody PlanHomePageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO clueDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        DeptDataPermissionDTO customerDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        BusinessDataPermission cluePermission = BeanUtils.copyBean(new BusinessDataPermission(), clueDataPermission);
        cluePermission.setSourceTable("clue");
        BusinessDataPermission customerPermission = BeanUtils.copyBean(new BusinessDataPermission(), customerDataPermission);
        customerPermission.setSourceTable("customer");
        return followUpPlanService.totalList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), List.of(cluePermission, customerPermission));
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除跟进计划")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE}, logical = Logical.OR)
    public void delete(@PathVariable String id) {
        followUpPlanService.delete(id);
    }

    @PostMapping("/status/update")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE}, logical = Logical.OR)
    @Operation(summary = "更新跟进计划状态")
    public void updateStatus(@Validated @RequestBody FollowUpPlanStatusRequest request) {
        followUpPlanService.updateStatus(request, SessionUtils.getUserId());
    }
}
