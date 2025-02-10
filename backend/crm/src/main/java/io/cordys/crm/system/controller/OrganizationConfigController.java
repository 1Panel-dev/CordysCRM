package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.dto.response.SyncOrganizationDTO;
import io.cordys.crm.system.service.OrganizationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import java.util.List;

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

    @PostMapping("/add/email")
    @Operation(summary = "新增邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void addEmail(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.addEmail(emailDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/update/email")
    @Operation(summary = "更新邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void updateEmail(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.updateEmail(emailDTO, SessionUtils.getUserId());
    }


    //获取同步组织设置
    @GetMapping("/synchronization")
    @Operation(summary = "获取同步组织设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public List<SyncOrganizationDTO> getSynOrganization() {
        return organizationConfigService.getSynOrganization(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add/synchronization")
    @Operation(summary = "新增同步组织设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void addSynchronization(@Validated @RequestBody SyncOrganizationDTO syncOrganizationDTO) {
        organizationConfigService.addSynchronization(syncOrganizationDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/update/synchronization")
    @Operation(summary = "更新同步组织设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void updateSynchronization(@Validated @RequestBody SyncOrganizationDTO syncOrganizationDTO) {
        organizationConfigService.updateSynchronization(syncOrganizationDTO, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除组织设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void delete(@PathVariable String id) {
        organizationConfigService.delete(id);
    }
}
