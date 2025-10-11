package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.sync.service.ThirdDepartmentService;
import cn.cordys.security.SessionUtils;
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
    private ThirdDepartmentService thirdDepartmentService;

    @GetMapping("/{type}")
    @RequiresPermissions(PermissionConstants.SYS_ORGANIZATION_SYNC)
    @Operation(summary = "用户(员工)-同步组织架构")
    public void syncUser(@PathVariable String type) {
        thirdDepartmentService.syncUser(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), type);
    }
}
