package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_PLAN.getKey(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @CsPermission(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "数据权限TAB")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return followUpPlanService.getTabEnableConfig(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "跟进记录列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody PlanHomePageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        DeptDataPermissionDTO dataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.FOLLOW_PLAN_READ);
        return followUpPlanService.totalList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                dataPermission);
    }

    @GetMapping("/delete/{id}")
    @CsPermission(value = PermissionConstants.FOLLOW_PLAN_DELETE, resourceId = "{#id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "删除跟进计划")
    public void delete(@PathVariable String id) {
        followUpPlanService.delete(id);
    }

    @PostMapping("/status/update")
    @CsPermission(value = PermissionConstants.FOLLOW_PLAN_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "更新跟进计划状态")
    public void updateStatus(@Validated @RequestBody FollowUpPlanStatusRequest request) {
        followUpPlanService.updateStatus(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/{id}")
    @CsPermission(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "跟进计划详情")
    public FollowUpPlanDetailResponse get(@PathVariable String id) {
        followUpPlanService.checkReadPermission(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        return followUpPlanService.get(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @CsPermission(value = PermissionConstants.FOLLOW_PLAN_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "更新跟进计划")
    public FollowUpPlan update(@Validated @RequestBody FollowUpPlanUpdateRequest request) {
        return followUpPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.FOLLOW_PLAN_ADD)
    @Operation(summary = "添加跟进计划")
    public FollowUpPlan add(@Validated @RequestBody FollowUpPlanAddRequest request) {
        return followUpPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
