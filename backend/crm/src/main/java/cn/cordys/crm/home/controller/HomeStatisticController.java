package cn.cordys.crm.home.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.home.dto.response.*;
import cn.cordys.crm.home.service.HomeStatisticService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "首页统计")
@RestController
@RequestMapping("/home/statistic")
public class HomeStatisticController {

    @Resource
    private HomeStatisticService homeStatisticService;

    @PostMapping("/customer")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户统计")
    public HomeCustomerStatistic getCustomerStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeCustomerStatistic() : homeStatisticService.getCustomerStatistic(wrapperRequest);
    }

    @PostMapping("/clue")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索统计")
    public HomeClueStatistic getClueStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeClueStatistic() : homeStatisticService.getClueStatistic(wrapperRequest);
    }

    @PostMapping("/opportunity")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "跟进商机统计")
    public HomeOpportunityStatistic getOpportunityStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeOpportunityStatistic() : homeStatisticService.getOpportunityStatistic(wrapperRequest);
    }

    @PostMapping("/contact")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人统计")
    public HomeContactStatistic getContactStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeContactStatistic() : homeStatisticService.getContactStatistic(wrapperRequest);
    }

    @PostMapping("/follow/record")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "跟进记录统计")
    public HomeFollowUpRecordStatistic getFollowUpRecordStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeFollowUpRecordStatistic() : homeStatisticService.getFollowUpRecordStatistic(wrapperRequest);
    }

    @PostMapping("/follow/plan")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "跟进计划统计")
    public HomeFollowUpPlanStatistic getFollowUpPlanStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        HomeStatisticSearchWrapperRequest wrapperRequest = homeStatisticService.getHomeStatisticSearchWrapperRequest(request);
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeFollowUpPlanStatistic() : homeStatisticService.getFollowUpPlanStatistic(wrapperRequest);
    }

    @GetMapping("/department/tree")
    @Operation(summary = "用户部门权限树")
    public List<BaseTreeNode> getDepartmentTree() {
        return homeStatisticService.getDepartmentTree(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
