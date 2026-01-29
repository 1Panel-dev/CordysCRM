package cn.cordys.crm.home.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.home.dto.request.HomeStatisticBaseSearchRequest;
import cn.cordys.crm.home.dto.response.HomeClueStatistic;
import cn.cordys.crm.home.dto.response.HomeOpportunityStatistic;
import cn.cordys.crm.home.service.HomeStatisticService;
import cn.cordys.crm.opportunity.constants.OpportunityStageScenario;
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

    @PostMapping("/lead")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索统计")
    public HomeClueStatistic getClueStatistic(@RequestBody @Validated HomeStatisticBaseSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.CLUE_MANAGEMENT_READ);
        return homeStatisticService.isEmptyDeptData(request.getSearchType(), deptDataPermission) ?
                new HomeClueStatistic() : homeStatisticService.getClueStatistic(request, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/opportunity")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机统计")
    public HomeOpportunityStatistic getOpportunityStatistic(@RequestBody @Validated HomeStatisticBaseSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return homeStatisticService.isEmptyDeptData(request.getSearchType(), deptDataPermission) ?
                new HomeOpportunityStatistic() : homeStatisticService.getOpportunityStatistic(request, null, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/opportunity/underway")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "进行中商机统计")
    public HomeOpportunityStatistic getUnderwayOpportunityStatistic(@RequestBody @Validated HomeStatisticBaseSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return homeStatisticService.isEmptyDeptData(request.getSearchType(), deptDataPermission) ?
                new HomeOpportunityStatistic() : homeStatisticService.getOpportunityStatistic(request, OpportunityStageScenario.UNDERWAY,
                deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/opportunity/success")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "赢单统计")
    public HomeOpportunityStatistic getSuccessOpportunityStatistic(@RequestBody @Validated HomeStatisticBaseSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return homeStatisticService.isEmptyDeptData(request.getSearchType(), deptDataPermission) ?
                new HomeOpportunityStatistic() : homeStatisticService.getOpportunityStatistic(request, OpportunityStageScenario.SUCCESS,
                deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/department/tree")
    @Operation(summary = "用户部门权限树")
    public List<BaseTreeNode> getDepartmentTree() {
        return homeStatisticService.getDepartmentTree(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
