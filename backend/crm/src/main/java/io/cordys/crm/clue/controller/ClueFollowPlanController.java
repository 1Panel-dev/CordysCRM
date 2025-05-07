package io.cordys.crm.clue.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.follow.domain.FollowUpPlan;
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

@Tag(name = "线索跟进计划")
@RestController
@RequestMapping("/clue/follow/plan")
public class ClueFollowPlanController {

    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "添加线索跟进计划")
    public FollowUpPlan add(@Validated @RequestBody FollowUpPlanAddRequest request) {
        return followUpPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索跟进计划")
    public FollowUpPlan update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE", null);
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索跟进计划详情")
    public FollowUpPlanDetailResponse get(@PathVariable String id) {
        return followUpPlanService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/cancel/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "取消线索跟进计划")
    public void cancelPlan(@PathVariable String id) {
        followUpPlanService.cancelPlan(id, SessionUtils.getUserId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "线索删除跟进计划")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    public void deletePlan(@PathVariable String id) {
        followUpPlanService.delete(id);
    }
}
