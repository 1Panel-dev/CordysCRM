package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.integration.wecom.service.WeComDepartmentService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/sync")
@Tag(name = "用户(员工)同步")
public class UserSyncController {

    @Resource
    private WeComDepartmentService weComDepartmentService;

    @GetMapping("/{type}")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    @Operation(summary = "用户(员工)-同步组织架构")
    public void syncUser(@PathVariable String type) {
        weComDepartmentService.syncUser(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
