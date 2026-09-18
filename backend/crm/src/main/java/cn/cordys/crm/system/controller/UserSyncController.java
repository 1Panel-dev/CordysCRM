package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.service.ThirdDepartmentService;
import cn.cordys.crm.system.dto.request.SyncUserRequest;
import cn.cordys.crm.system.dto.request.schedule.SyncUserScheduleConfigRequest;
import cn.cordys.crm.system.dto.response.SyncUserScheduleConfigResponse;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/sync")
@Tag(name = "用户(员工)同步")
public class UserSyncController {

    @Resource
    private ThirdDepartmentService thirdDepartmentService;

    @PostMapping("/operation")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    @Operation(summary = "用户(员工)-同步组织架构")
    public void syncUser(@Validated @RequestBody SyncUserRequest request) {
        thirdDepartmentService.syncUser(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/check")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    @Operation(summary = "用户(员工)-检查是否还在同步组织架构")
    public Boolean checkSyncUser() {
        return thirdDepartmentService.getSyncStatus(OrganizationContext.getOrganizationId());
    }


    @PostMapping(value = "/schedule-config")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    @Operation(summary = "保存用户(员工)-同步组织架构定时任务配置")
    public void scheduleConfig(@Validated @RequestBody SyncUserScheduleConfigRequest request) {
        thirdDepartmentService.scheduleConfig(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/schedule-config/{type}")
    @Operation(summary = "获取用户(员工)-同步组织架构定时任务配置")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    public SyncUserScheduleConfigResponse getScheduleConfig(@PathVariable String type) {
        return thirdDepartmentService.getScheduleConfig(type, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/third-org/{type}")
    @Operation(summary = "获取第三方的组织架构")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    public List<ThirdDepartment> getThirdOrg(@PathVariable String type) {
        return thirdDepartmentService.getThirdOrg(type, OrganizationContext.getOrganizationId());
    }
}
