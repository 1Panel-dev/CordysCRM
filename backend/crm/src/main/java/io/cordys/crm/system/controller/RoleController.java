package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.security.SessionUtils;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.*;

import io.cordys.crm.system.service.RoleService;
import io.cordys.common.pager.PageUtils;
import java.util.List;

/**
 * 角色
 *
 * @author jianxing
 * @date 2025-01-03 14:13:46
 */
@RestController
@RequestMapping("/role")
public class RoleController {
    @Resource
    private RoleService roleService;

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.SYSTEM_ROLE_READ)
    @Operation(summary = "角色列表")
    public Pager<List<RoleListResponse>> list(@Validated @RequestBody RolePageRequest request){
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, roleService.list(request));
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
        return roleService.add(request, SessionUtils.getUserId());
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
}
