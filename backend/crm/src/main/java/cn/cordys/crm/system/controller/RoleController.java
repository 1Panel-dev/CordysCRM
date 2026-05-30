package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.PermissionDefinitionItem;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.Role;
import cn.cordys.crm.system.dto.request.RoleAddRequest;
import cn.cordys.crm.system.dto.request.RoleUpdateRequest;
import cn.cordys.crm.system.dto.request.RoleUserPageRequest;
import cn.cordys.crm.system.dto.request.RoleUserRelateRequest;
import cn.cordys.crm.system.dto.response.RoleGetResponse;
import cn.cordys.crm.system.dto.response.RoleListResponse;
import cn.cordys.crm.system.dto.response.RoleUserListResponse;
import cn.cordys.crm.system.dto.response.RoleUserOptionResponse;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.crm.system.service.UserRoleService;
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

/**
 * @author jianxing
 * @date 2025-01-03 16:52:34
 */
@Tag(name = "角色")
@RestController
@RequestMapping("/role")
public class RoleController {
    @Resource
    private RoleService roleService;
    @Resource
    private UserRoleService userRoleService;
    @Resource
    private DepartmentService departmentService;

    @GetMapping("/list")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    @Operation(summary = "角色列表")
    public List<RoleListResponse> list() {
        return roleService.list(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    @Operation(summary = "角色详情")
    public RoleGetResponse get(@PathVariable String id) {
        return roleService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD)
    @Operation(summary = "添加角色")
    public Role add(@Validated @RequestBody RoleAddRequest request) {
        return roleService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_UPDATE)
    @Operation(summary = "更新角色")
    public Role update(@Validated @RequestBody RoleUpdateRequest request) {
        return roleService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除角色")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_DELETE)
    public void delete(@PathVariable String id) {
        roleService.delete(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/permission/setting")
    @Operation(summary = "获取权限配置")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    public List<PermissionDefinitionItem> getPermissionSetting() {
        return roleService.getPermissionSetting();
    }

    @PostMapping("/user/page")
    @Operation(summary = "查看拥有该权限的用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    public Pager<List<RoleUserListResponse>> listUser(@Validated @RequestBody RoleUserPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, userRoleService.listUserByRoleId(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/dept/tree")
    @Operation(summary = "获取部门树")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    public List<BaseTreeNode> getDeptTree() {
        return departmentService.getTree(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/dept/tree/{roleId}")
    @Operation(summary = "获取部门用户树")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD_USER)
    public List<DeptUserTreeNode> getDeptUserTree(@PathVariable String roleId) {
        return userRoleService.getDeptUserTree(OrganizationContext.getOrganizationId(), roleId);
    }

    @GetMapping("/user/role/tree/{roleId}")
    @Operation(summary = "获取角色用户树")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD_USER)
    public List<RoleUserTreeNode> getRoleUserTree(@PathVariable String roleId) {
        return userRoleService.getRoleUserTree(OrganizationContext.getOrganizationId(), roleId);
    }

    @GetMapping("/user/option/{roleId}")
    @Operation(summary = "获取所有用户选项")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD_USER)
    public List<RoleUserOptionResponse> RoleUserOptionResponse(@PathVariable String roleId) {
        return userRoleService.getUserOptionByRoleId(OrganizationContext.getOrganizationId(), roleId);
    }

    @PostMapping("/user/relate")
    @Operation(summary = "角色关联添加用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_ADD_USER)
    public void relateUser(@Validated @RequestBody RoleUserRelateRequest request) {
        userRoleService.relateUser(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/delete/{id}")
    @Operation(summary = "角色移除用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_REMOVE_USER)
    public void deleteRoleUser(@PathVariable String id) {
        userRoleService.deleteRoleUser(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/user/batch/delete")
    @Operation(summary = "角色批量移除用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_REMOVE_USER)
    public void batchDeleteRoleUser(@RequestBody List<String> ids) {
        userRoleService.batchDeleteRoleUser(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
