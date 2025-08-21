package io.cordys.crm.dashboard.controller;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.dashboard.domain.Dashboard;
import io.cordys.crm.dashboard.dto.request.*;
import io.cordys.crm.dashboard.dto.response.DashboardDetailResponse;
import io.cordys.crm.dashboard.dto.response.DashboardPageResponse;
import io.cordys.crm.dashboard.service.DashboardService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "仪表板")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.DASHBOARD_ADD)
    @Operation(summary = "仪表板-添加")
    public Dashboard addDashboard(@Validated @RequestBody DashboardAddRequest request) {
        return dashboardService.addDashboard(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @GetMapping("/detail/{id}")
    @Operation(summary = "仪表板-详情")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    public DashboardDetailResponse getDashboardDetail(@PathVariable String id) {
        return dashboardService.getDashboardDetail(id);
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.DASHBOARD_EDIT)
    @Operation(summary = "仪表板-更新")
    public void updateDashboard(@Validated @RequestBody DashboardUpdateRequest request) {
        dashboardService.updateDashboard(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/rename")
    @RequiresPermissions(PermissionConstants.DASHBOARD_EDIT)
    @Operation(summary = "仪表板-重命名")
    public void rename(@Validated @RequestBody DashboardRenameRequest request) {
        dashboardService.rename(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.DASHBOARD_DELETE)
    @Operation(summary = "仪表板-删除")
    public void delete(@PathVariable String id) {
        dashboardService.delete(id);
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "仪表板列表")
    public Pager<List<DashboardPageResponse>> list(@Validated @RequestBody DashboardPageRequest request) {
        return dashboardService.getList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/collect/{id}")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "仪表板收藏")
    public void collect(@PathVariable String id) {
        dashboardService.collect(id, SessionUtils.getUserId());
    }


    @GetMapping("/un-collect/{id}")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "仪表板取消收藏")
    public void unCollect(@PathVariable String id) {
        dashboardService.unCollect(id, SessionUtils.getUserId());
    }


    @PostMapping("/collect/page")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "仪表板收藏列表")
    public Pager<List<DashboardPageResponse>> collectPage(@Validated @RequestBody BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, dashboardService.collectList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId()));
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "仪表板-拖拽排序")
    @RequiresPermissions(PermissionConstants.DASHBOARD_EDIT)
    public void editPos(@Validated @RequestBody DashboardEditPosRequest request) {
        dashboardService.editPos(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
