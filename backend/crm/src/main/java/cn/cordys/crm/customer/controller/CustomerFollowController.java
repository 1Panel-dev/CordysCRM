package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.service.PoolCustomerService;
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

@Tag(name = "客户跟进")
@RestController
@RequestMapping("/account/follow")
public class CustomerFollowController {

    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private RelatedResourcePermissionService relatedResourcePermissionService;
    @Resource
    private DataScopeService dataScopeService;

    @PostMapping("/plan/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.CUSTOMER)
    @RequiresPermissions(PermissionConstants.FOLLOW_PLAN_READ)
    @Operation(summary = "客户跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> planPage(
            @Validated @RequestBody FollowUpPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_PLAN.getKey());
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        DeptDataPermissionDTO roleScope = dataScopeService.getDeptDataPermission(userId, orgId,
                InternalUserView.ALL.name(), PermissionConstants.FOLLOW_PLAN_READ);
        request.setCustomerTabScope(customerTabScope(request.getSourceId(), roleScope, userId, orgId));
        return followUpPlanService.list(request, userId, orgId, "CUSTOMER", "CUSTOMER");
    }

    @PostMapping("/record/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_READ, resourceId = "{#request.sourceId}", formType = FormKeyConstants.CUSTOMER)
    @RequiresPermissions(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "客户跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> recordPage(
            @Validated @RequestBody FollowUpRecordPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        DeptDataPermissionDTO roleScope = dataScopeService.getDeptDataPermission(userId, orgId,
                InternalUserView.ALL.name(), PermissionConstants.FOLLOW_RECORD_READ);
        request.setCustomerTabScope(customerTabScope(request.getSourceId(), roleScope, userId, orgId));
        return followUpRecordService.list(request, userId, orgId, "CUSTOMER", "CUSTOMER");
    }

    @PostMapping("/record/pool/page")
    @RequiresPermissions(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "客户公海跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> recordPoolPage(
            @Validated @RequestBody FollowUpRecordPageRequest request) {
        poolCustomerService.checkPoolPermissionByCustomerId(request.getSourceId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return followUpRecordService.poolList(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), "CUSTOMER", "CUSTOMER");
    }

    private DeptDataPermissionDTO customerTabScope(String customerId, DeptDataPermissionDTO roleScope, String userId,
            String orgId) {
        DeptDataPermissionDTO collaborationScope = relatedResourcePermissionService.getCustomerTabDataPermission(
                customerId, userId, orgId);
        return relatedResourcePermissionService.mergeCustomerTabDataPermission(roleScope, collaborationScope, orgId);
    }

}





