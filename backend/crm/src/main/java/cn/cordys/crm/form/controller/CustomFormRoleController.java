package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleListResponse;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import cn.cordys.crm.form.service.CustomFormRoleService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "自定义表单角色")
@RestController
@RequestMapping("/custom-form/role")
public class CustomFormRoleController {

    @Resource
    private CustomFormRoleService customFormRoleService;

    @GetMapping("/list/{customFormId}")
    @Operation(summary = "获取表单角色列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<CustomFormRoleListResponse> listByFormId(@PathVariable String customFormId) {
        return customFormRoleService.listByFormId(customFormId, SessionUtils.getUserId());
    }

    @PostMapping("/users")
    @Operation(summary = "获取角色用户列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public Pager<List<CustomFormRoleUserListResponse>> listUsersByRole(@Validated @RequestBody CustomFormRoleUserPageRequest request) {
        return customFormRoleService.listUsersByRole(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/dept/tree")
    @Operation(summary = "获取部门用户树")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<DeptUserTreeNode> getDeptUserTree() {
        return customFormRoleService.getDeptUserTree(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/role/tree")
    @Operation(summary = "获取角色用户树")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<RoleUserTreeNode> getRoleUserTree() {
        return customFormRoleService.getRoleUserTree(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/user/add")
    @Operation(summary = "角色添加用户")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void addUsers(@Validated @RequestBody CustomFormRoleUserBatchRequest request) {
        customFormRoleService.addUsers(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/user/remove")
    @Operation(summary = "角色移除用户")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void removeUsers(@RequestBody CustomFormRoleUserBatchRequest request) {
        customFormRoleService.removeUsers(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
