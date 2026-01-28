package cn.cordys.crm.dashboard.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.dashboard.domain.DashboardModule;
import cn.cordys.crm.dashboard.dto.DashboardTreeNode;
import cn.cordys.crm.dashboard.dto.request.DashboardModuleAddRequest;
import cn.cordys.crm.dashboard.dto.request.DashboardModuleRenameRequest;
import cn.cordys.crm.dashboard.service.DashboardModuleService;
import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "仪表板模块")
@RestController
@RequestMapping("/dashboard/module")
public class DashboardModuleController {

  @Resource private DashboardModuleService dashboardModuleService;

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.DASHBOARD_ADD)
  @Operation(summary = "仪表板-添加文件夹")
  public DashboardModule addFileModule(@Validated @RequestBody DashboardModuleAddRequest request) {
    return dashboardModuleService.addFileModule(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/rename")
  @RequiresPermissions(PermissionConstants.DASHBOARD_EDIT)
  @Operation(summary = "仪表板-重命名文件夹")
  public void rename(@Validated @RequestBody DashboardModuleRenameRequest request) {
    dashboardModuleService.rename(request, SessionUtils.getUserId());
  }

  @PostMapping("/delete")
  @RequiresPermissions(PermissionConstants.DASHBOARD_DELETE)
  @Operation(summary = "仪表板-刪除文件夹")
  public void deleteDashboardModule(@RequestBody @NotEmpty List<String> ids) {
    dashboardModuleService.delete(
        ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/tree")
  @Operation(summary = "仪表板-文件树查询")
  @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
  public List<DashboardTreeNode> getTree() {
    return dashboardModuleService.getTree(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/count")
  @Operation(summary = "仪表板-文件树数量")
  @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
  public Map<String, Long> moduleCount() {
    return dashboardModuleService.moduleCount(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/move")
  @Operation(summary = "仪表板-文件夹移动")
  @RequiresPermissions(PermissionConstants.DASHBOARD_EDIT)
  public void moveNode(@Validated @RequestBody NodeMoveRequest request) {
    dashboardModuleService.moveNode(request, SessionUtils.getUserId());
  }
}
