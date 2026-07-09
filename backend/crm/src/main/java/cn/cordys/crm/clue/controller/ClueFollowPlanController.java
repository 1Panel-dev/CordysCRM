package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.util.BeanUtils;
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

@Tag(name = "线索跟进计划")
@RestController
@RequestMapping("/lead/follow/plan")
public class ClueFollowPlanController {

    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/add")
    @Operation(summary = "添加线索跟进计划")
    public FollowUpPlan add(@Validated @RequestBody FollowUpPlanAddRequest request) {
        followUpPlanService.checkPlanPermission(BeanUtils.copyBean(new FollowUpPlan(), request),
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), false);
        return followUpPlanService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新线索跟进计划")
    public FollowUpPlan update(@Validated @RequestBody FollowUpPlanUpdateRequest request) {
        followUpPlanService.checkUpdatePermission(request.getId(), SessionUtils.getUserId());
        return followUpPlanService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @Operation(summary = "线索跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        FollowUpPlan followPlanRecord = new FollowUpPlan();
        followPlanRecord.setClueId(request.getSourceId());
        followPlanRecord.setType(ModuleKey.CLUE.name());
        followUpPlanService.checkPlanPermission(followPlanRecord, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE");
    }


    @GetMapping("/get/{id}")
    @Operation(summary = "线索跟进计划详情")
    public FollowUpPlanDetailResponse get(@PathVariable String id) {
        followUpPlanService.checkPlanPermission(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return followUpPlanService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/cancel/{id}")
    @Operation(summary = "取消线索跟进计划")
    public void cancelPlan(@PathVariable String id) {
        followUpPlanService.checkUpdatePermission(id, SessionUtils.getUserId());
        followUpPlanService.cancelPlan(id, SessionUtils.getUserId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "线索删除跟进计划")
    public void deletePlan(@PathVariable String id) {
        followUpPlanService.checkUpdatePermission(id, SessionUtils.getUserId());
        followUpPlanService.delete(id);
    }


    @PostMapping("/status/update")
    @Operation(summary = "线索更新跟进计划状态")
    public void updateStatus(@Validated @RequestBody FollowUpPlanStatusRequest request) {
        followUpPlanService.checkUpdatePermission(request.getId(), SessionUtils.getUserId());
        followUpPlanService.updateStatus(request, SessionUtils.getUserId());
    }
}
