package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.dto.request.DepartmentAddRequest;
import cn.cordys.crm.system.dto.request.DepartmentCommanderRequest;
import cn.cordys.crm.system.dto.request.DepartmentRenameRequest;
import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
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
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_READ)
    public List<BaseTreeNode> getTree() {
        return departmentService.getTree(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_ADD)
    @Operation(summary = "组织架构-添加子部门")
    public Department addDepartment(@Validated @RequestBody DepartmentAddRequest request) {
        return departmentService.addDepartment(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/rename")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_UPDATE)
    @Operation(summary = "组织架构-重命名子部门")
    public void rename(@Validated @RequestBody DepartmentRenameRequest request) {
        departmentService.rename(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/set-commander")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_UPDATE)
    @Operation(summary = "组织架构-设置部门负责人")
    public void setCommander(@Validated @RequestBody DepartmentCommanderRequest request) {
        departmentService.setCommander(request, SessionUtils.getUserId());
    }

    @PostMapping("/delete/check")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_DELETE)
    @Operation(summary = "组织架构-删除部门校验")
    public boolean deleteCheck(@RequestBody @NotEmpty List<String> ids) {
        return departmentService.deleteCheck(ids, OrganizationContext.getOrganizationId());

    }


    @PostMapping("/delete")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_DELETE)
    @Operation(summary = "组织架构-删除部门")
    public void deleteDepartment(@RequestBody @NotEmpty List<String> ids) {
        departmentService.delete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());

    }


    @PostMapping("/sort")
    @Operation(summary = "组织架构-部门排序")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_UPDATE)
    public void sort(@Validated @RequestBody NodeMoveRequest request) {
        departmentService.sort(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
