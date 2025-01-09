package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.dto.request.RoleAddRequest;
import io.cordys.crm.system.dto.request.RoleUpdateRequest;
import io.cordys.crm.system.dto.response.RoleGetResponse;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.crm.system.service.RoleService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-01-03 16:52:34
 */
@Tag(name = "角色")
@RestController
@RequestMapping("/role")
public class RoleController {
    @Resource
    private RoleService roleService;

    @GetMapping("/list")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    @Operation(summary = "角色列表")
    public List<RoleListResponse> list(){
        return roleService.list();
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    @Operation(summary = "角色详情")
    public RoleGetResponse get(@PathVariable String id){
        return roleService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD)
    @Operation(summary = "添加角色")
    public Role add(@Validated @RequestBody RoleAddRequest request){
        return roleService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_UPDATE)
    @Operation(summary = "更新角色")
    public Role update(@Validated @RequestBody RoleUpdateRequest request){
        return roleService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_DELETE)
    public void delete(@PathVariable String id){
        roleService.delete(id);
    }

    @GetMapping("/permission/setting/{id}")
    @Operation(summary = "获取角色对应的权限配置")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    public List<PermissionDefinitionItem> getPermissionSetting(@PathVariable String id) {
        return roleService.getPermissionSetting(id);
    }
}
