package cn.cordys.crm.integration.tender.controller;

import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "标讯")
@RestController
@RequestMapping("/tender")
public class TenderController {


    @Resource
    private IntegrationConfigService integrationConfigService;

    @GetMapping("/application/config")
    @Operation(summary = "获取标讯设置")
    @RequiresPermissions(PermissionConstants.TENDER_READ)
    public ThirdConfigurationDTO getApplicationConfig() {
        return integrationConfigService.getApplicationConfig(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), DepartmentConstants.TENDER.name());
    }
}
