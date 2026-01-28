package cn.cordys.crm.integration.agent.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.agent.domain.AgentModule;
import cn.cordys.crm.integration.agent.dto.request.AgentModuleAddRequest;
import cn.cordys.crm.integration.agent.dto.request.AgentModuleRenameRequest;
import cn.cordys.crm.integration.agent.service.AgentModuleService;
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

@Tag(name = "智能体模块")
@RestController
@RequestMapping("/agent/module")
public class AgentModuleController {

  @Resource private AgentModuleService agentModuleService;

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.AGENT_ADD)
  @Operation(summary = "智能体-添加文件夹")
  public AgentModule addFileModule(@Validated @RequestBody AgentModuleAddRequest request) {
    return agentModuleService.addFileModule(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/rename")
  @RequiresPermissions(PermissionConstants.AGENT_UPDATE)
  @Operation(summary = "智能体-重命名文件夹")
  public void rename(@Validated @RequestBody AgentModuleRenameRequest request) {
    agentModuleService.rename(request, SessionUtils.getUserId());
  }

  @PostMapping("/delete")
  @RequiresPermissions(PermissionConstants.AGENT_DELETE)
  @Operation(summary = "智能体-刪除文件夹")
  public void deleteAgentModule(@RequestBody @NotEmpty List<String> ids) {
    agentModuleService.delete(
        ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/tree")
  @Operation(summary = "智能体-文件树查询")
  @RequiresPermissions(PermissionConstants.AGENT_READ)
  public List<BaseTreeNode> getTree() {
    return agentModuleService.getTree(OrganizationContext.getOrganizationId());
  }

  @GetMapping("/count")
  @Operation(summary = "智能体-文件树数量")
  @RequiresPermissions(PermissionConstants.AGENT_READ)
  public Map<String, Long> moduleCount() {
    return agentModuleService.moduleCount(
        SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/move")
  @Operation(summary = "智能体-文件夹移动")
  @RequiresPermissions(PermissionConstants.AGENT_UPDATE)
  public void moveNode(@Validated @RequestBody NodeMoveRequest request) {
    agentModuleService.moveNode(request, SessionUtils.getUserId());
  }
}
