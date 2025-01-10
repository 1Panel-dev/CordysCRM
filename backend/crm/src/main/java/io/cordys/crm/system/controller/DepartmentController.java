package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.request.DepartmentAddRequest;
import io.cordys.crm.system.dto.request.DepartmentCommanderRequest;
import io.cordys.crm.system.dto.request.DepartmentRenameRequest;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/department")
@Tag(name = "组织架构")
public class DepartmentController {
    @Resource
    private DepartmentService departmentService;

    @GetMapping("/tree")
    @Operation(summary = "组织架构-部门树查询")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_READ)
    public List<BaseTreeNode> getTree() {
        return departmentService.getTree(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_ADD)
    @Operation(summary = "组织架构-添加子部门")
    public void addDepartment(@Validated @RequestBody DepartmentAddRequest request) {
        departmentService.addDepartment(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/rename")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "组织架构-重命名子部门")
    public void rename(@Validated @RequestBody DepartmentRenameRequest request) {
        departmentService.rename(request, SessionUtils.getUserId());
    }


    @PostMapping("/set-commander")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "组织架构-设置部门负责人")
    public void setCommander(@Validated @RequestBody DepartmentCommanderRequest request) {
        departmentService.setCommander(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/check/{id}")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "组织架构-删除部门校验")
    public boolean deleteCheck(@PathVariable String id) {
        return departmentService.deleteCheck(id, OrganizationContext.getOrganizationId());

    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_DELETE)
    @Operation(summary = "组织架构-删除部门")
    public void deleteDepartment(@PathVariable String id) {
        departmentService.delete(id, OrganizationContext.getOrganizationId());

    }
    //todo  排序 num值

}
