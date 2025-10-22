package cn.cordys.crm.integration.agent.controller;


import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.agent.domain.Agent;
import cn.cordys.crm.integration.agent.dto.AgentOptionDTO;
import cn.cordys.crm.integration.agent.dto.request.*;
import cn.cordys.crm.integration.agent.dto.response.AgentDetailResponse;
import cn.cordys.crm.integration.agent.dto.response.AgentPageResponse;
import cn.cordys.crm.integration.agent.dto.response.ScriptResponse;
import cn.cordys.crm.integration.agent.service.AgentBaseService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "智能体")
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentBaseService agentBaseService;

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.AGENT_ADD)
    @Operation(summary = "智能体-添加")
    public Agent addAgent(@Validated @RequestBody AgentAddRequest request) {
        return agentBaseService.addAgent(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @GetMapping("/detail/{id}")
    @Operation(summary = "智能体-详情")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    public AgentDetailResponse getDetail(@PathVariable String id) {
        return agentBaseService.getDetail(id);
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.AGENT_UPDATE)
    @Operation(summary = "智能体-更新")
    public void updateAgent(@Validated @RequestBody AgentUpdateRequest request) {
        agentBaseService.updateAgent(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/rename")
    @RequiresPermissions(PermissionConstants.AGENT_UPDATE)
    @Operation(summary = "智能体-重命名")
    public void rename(@Validated @RequestBody AgentRenameRequest request) {
        agentBaseService.rename(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.AGENT_DELETE)
    @Operation(summary = "智能体-删除")
    public void delete(@PathVariable String id) {
        agentBaseService.delete(id);
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    @Operation(summary = "智能体列表")
    public Pager<List<AgentPageResponse>> list(@Validated @RequestBody AgentPageRequest request) {
        return agentBaseService.getList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/collect/{id}")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    @Operation(summary = "智能体收藏")
    public void collect(@PathVariable String id) {
        agentBaseService.collect(id, SessionUtils.getUserId());
    }


    @GetMapping("/un-collect/{id}")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    @Operation(summary = "智能体取消收藏")
    public void unCollect(@PathVariable String id) {
        agentBaseService.unCollect(id, SessionUtils.getUserId());
    }


    @PostMapping("/collect/page")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    @Operation(summary = "智能体收藏列表")
    public Pager<List<AgentPageResponse>> collectPage(@Validated @RequestBody BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, agentBaseService.collectList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId()));
    }


    @PostMapping("/edit/pos")
    @Operation(summary = "智能体-拖拽排序")
    @RequiresPermissions(PermissionConstants.AGENT_UPDATE)
    public void editPos(@Validated @RequestBody AgentEditPosRequest request) {
        agentBaseService.editPos(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping(value = "/option")
    @Operation(summary = "用户可选智能体下拉option")
    public List<AgentOptionDTO> getAgentList() {
        return agentBaseService.getAgentOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/check")
    @Operation(summary = "配置连接检测")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    public Boolean checkConfig() {
        return agentBaseService.checkConfig(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/workspace")
    @Operation(summary = "获取用户工作空间")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    public List<OptionDTO> getWorkspace() {
        return agentBaseService.workspace(OrganizationContext.getOrganizationId());
    }


    @GetMapping("/application/{workspaceId}")
    @Operation(summary = "获取智能体应用")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    public List<OptionDTO> getApplication(@PathVariable String workspaceId) {
        return agentBaseService.application(workspaceId, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/script")
    @Operation(summary = "获取脚本信息")
    @RequiresPermissions(PermissionConstants.AGENT_READ)
    public ScriptResponse getScript(@Validated @RequestBody ScriptRequest request) {
        return agentBaseService.script(request, OrganizationContext.getOrganizationId());
    }
}
