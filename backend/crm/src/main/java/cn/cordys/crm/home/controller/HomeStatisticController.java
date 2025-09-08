package cn.cordys.crm.home.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.home.dto.response.HomeClueStatistic;
import cn.cordys.crm.home.dto.response.HomeOpportunityStatistic;
import cn.cordys.crm.home.dto.response.HomeSuccessOpportunityStatistic;
import cn.cordys.crm.home.service.HomeStatisticService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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

    @PostMapping("/clue")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索统计")
    public HomeClueStatistic getClueStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.CLUE_MANAGEMENT_READ);
        HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeClueStatistic() : homeStatisticService.getClueStatistic(wrapperRequest);
    }

    @PostMapping("/opportunity")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "跟进商机统计")
    public HomeOpportunityStatistic getOpportunityStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeOpportunityStatistic() : homeStatisticService.getOpportunityStatistic(wrapperRequest);
    }

    @PostMapping("/opportunity/success")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "赢单统计")
    public HomeSuccessOpportunityStatistic getSuccessOpportunityStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        return homeStatisticService.isEmptyDeptData(wrapperRequest) ?
                new HomeSuccessOpportunityStatistic() : homeStatisticService.getSuccessOpportunityStatistic(wrapperRequest);
    }

    @GetMapping("/department/tree")
    @Operation(summary = "用户部门权限树")
    public List<BaseTreeNode> getDepartmentTree() {
        return homeStatisticService.getDepartmentTree(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
