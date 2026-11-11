package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.service.PoolClueService;
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

@Tag(name = "线索跟进")
@RestController
@RequestMapping("/lead/follow")
public class ClueFollowController {

    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private PoolClueService poolClueService;

    @PostMapping("/plan/page")
    @CsPermission(value = PermissionConstants.CLUE_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.CLUE)
    @RequiresPermissions(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "线索跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> planPage(
            @Validated @RequestBody FollowUpPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        return followUpPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "CLUE", "CLUE");
    }

    @PostMapping("/record/page")
    @CsPermission(value = PermissionConstants.CLUE_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.CLUE)
    @RequiresPermissions(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "线索跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> recordPage(
            @Validated @RequestBody FollowUpRecordPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "CLUE", "CLUE");
    }

    @PostMapping("/record/pool/page")
    @RequiresPermissions(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "线索池跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> recordPoolPage(
            @Validated @RequestBody FollowUpRecordPageRequest request) {
        poolClueService.checkPoolPermissionByClueId(request.getSourceId(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        return followUpRecordService.poolList(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), "CLUE", "CLUE");
    }

}





