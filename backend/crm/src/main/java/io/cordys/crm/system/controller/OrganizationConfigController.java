package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.service.OrganizationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.cordys.security.SessionUtils;

@Tag(name = "企业设置")
@RestController
@RequestMapping("/organization/config")
public class OrganizationConfigController {

    @Resource
    private OrganizationConfigService organizationConfigService;

    //获取邮件设置
    @GetMapping("/email")
    @Operation(summary = "获取邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public EmailDTO getEmail() {
        return organizationConfigService.getEmail(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/edit/email")
    @Operation(summary = "编辑邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void editEmail(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.editEmail(emailDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/test/email")
    @Operation(summary = "系统设置-系统-系统参数-基本设置-邮件设置-测试连接")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public void verifyEmailConnection(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.verifyEmailConnection(emailDTO);
    }

}
