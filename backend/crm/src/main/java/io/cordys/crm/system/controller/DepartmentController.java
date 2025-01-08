package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.crm.system.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return departmentService.getTree();
    }

}
