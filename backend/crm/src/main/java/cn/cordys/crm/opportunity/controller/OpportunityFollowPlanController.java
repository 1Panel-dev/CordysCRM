package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机跟进计划")
@RestController
@RequestMapping("/opportunity/follow/plan")
public class OpportunityFollowPlanController {

    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/add")
    @CsPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "添加商机跟进计划")
    public FollowUpPlan add(@Validated @RequestBody FollowUpPlanAddRequest request) {
        return followUpPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "更新商机跟进计划")
    public FollowUpPlan update(@Validated @RequestBody FollowUpPlanUpdateRequest request) {
        return followUpPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @CsPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "OPPORTUNITY", "CUSTOMER");
    }


    @GetMapping("/get/{id}")
    @Operation(summary = "商机跟进计划详情")
    public FollowUpPlanDetailResponse get(@PathVariable String id) {
        return followUpPlanService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/cancel/{id}")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, resourceId = "{#id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "取消商机跟进计划")
    public void cancelPlan(@PathVariable String id) {
        followUpPlanService.cancelPlan(id, SessionUtils.getUserId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "商机删除跟进计划")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, resourceId = "{#id}", formType = FormKeyConstants.FOLLOW_PLAN)
    public void deletePlan(@PathVariable String id) {
        followUpPlanService.delete(id);
    }

    @PostMapping("/status/update")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_PLAN)
    @Operation(summary = "商机更新跟进计划状态")
    public void updateStatus(@Validated @RequestBody FollowUpPlanStatusRequest request) {
        followUpPlanService.updateStatus(request, SessionUtils.getUserId());
    }

}
