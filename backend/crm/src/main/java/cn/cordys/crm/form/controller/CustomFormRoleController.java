package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleListResponse;
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

    @GetMapping("/users/{roleId}")
    @Operation(summary = "获取角色用户列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<String> listUsersByRole(@PathVariable String roleId) {
        return customFormRoleService.listUsersByRole(roleId, SessionUtils.getUserId());
    }

    @PostMapping("/user/add")
    @Operation(summary = "角色添加用户")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void addUsers(@Validated @RequestBody CustomFormRoleUserBatchRequest request) {
        customFormRoleService.addUsers(request, SessionUtils.getUserId());
    }

    @PostMapping("/user/remove")
    @Operation(summary = "角色移除用户")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void removeUsers(@RequestBody CustomFormRoleUserBatchRequest request) {
        customFormRoleService.removeUsers(request, SessionUtils.getUserId());
    }
}
