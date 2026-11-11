package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商机跟进")
@RestController
@RequestMapping("/opportunity/follow")
public class OpportunityFollowController {

    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/plan/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.OPPORTUNITY)
    @RequiresPermissions(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "商机跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> planPage(
            @Validated @RequestBody FollowUpPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "OPPORTUNITY", "CUSTOMER");
    }

    @PostMapping("/record/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.OPPORTUNITY)
    @RequiresPermissions(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "商机跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> recordPage(
            @Validated @RequestBody FollowUpRecordPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "OPPORTUNITY", "CUSTOMER");
    }

}





