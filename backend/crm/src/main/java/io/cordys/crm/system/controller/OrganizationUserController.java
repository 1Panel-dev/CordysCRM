package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.constants.UserSource;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.UserImportResponse;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.service.OrganizationUserService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/user")
@Tag(name = "用户(员工)")
public class OrganizationUserController {

    @Resource
    private OrganizationUserService organizationUserService;


    @PostMapping("/list")
    @Operation(summary = "用户(员工)-列表查询")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_READ)
    public Pager<List<UserPageResponse>> list(@Validated @RequestBody UserPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, organizationUserService.list(request));
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_ADD)
    @Operation(summary = "用户(员工)-添加员工")
    public void addUser(@Validated @RequestBody UserAddRequest request) {
        organizationUserService.addUser(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @GetMapping("/detail/{id}")
    @Operation(summary = "用户(员工)-员工详情")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_READ)
    public UserResponse getUserDetail(@PathVariable String id) {
        return organizationUserService.getUserDetail(id);
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "用户(员工)-更新")
    public void updateUser(@Validated @RequestBody UserUpdateRequest request) {
        organizationUserService.updateUser(request, SessionUtils.getUserId());
    }


    @GetMapping("/reset-password/{userId}")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "用户(员工)-重置密码")
    public void resetPassword(@PathVariable String userId) {
        organizationUserService.resetPassword(userId, SessionUtils.getUserId());
    }


    @PostMapping("/batch-enable")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    @Operation(summary = "用户(员工)-批量启用/禁用")
    public void enable(@Validated @RequestBody UserBatchEnableRequest request) {
        organizationUserService.enable(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/reset-password")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_USER_RESET_PASSWORD)
    @Operation(summary = "用户(员工)-批量重置密码")
    public void batchResetPassword(@Validated @RequestBody UserBatchRequest request) {
        organizationUserService.batchResetPassword(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/batch/edit")
    @Operation(summary = "用户(员工)-批量编辑")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_UPDATE)
    public void batchEditUser(@Validated @RequestBody UserBatchEditRequest request) {
        organizationUserService.batchEditUser(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/download/template")
    @Operation(summary = "用户(员工)-excel导入-下载模板")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_ADD)
    public void userTemplateExport(HttpServletResponse response) {
        organizationUserService.downloadExcelTemplate(response);
    }


    @PostMapping("/import/pre-check")
    @Operation(summary = "用户(员工)-excel导入检查")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_IMPORT)
    public UserImportResponse preCheckXMind(@RequestPart(value = "file", required = false) MultipartFile file) {
        return organizationUserService.preCheck(file, OrganizationContext.getOrganizationId());
    }


    @PostMapping(value = "/import")
    @Operation(summary = "用户(员工)-excel导入")
    @RequiresPermissions(PermissionConstants.SYS_DEPARTMENT_IMPORT)
    public UserImportResponse importUser(@RequestPart(value = "file", required = false) MultipartFile file) {
        return organizationUserService.importByExcel(file, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
