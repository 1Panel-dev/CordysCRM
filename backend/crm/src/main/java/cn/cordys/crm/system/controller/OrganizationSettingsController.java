package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.dataease.dto.DeAuthDTO;
import cn.cordys.crm.integration.dataease.service.DataEaseService;
import cn.cordys.crm.integration.dataease.service.DataEaseSyncService;
import cn.cordys.crm.system.dto.response.EmailDTO;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.OrganizationConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Tag(name = "企业设置")
@RestController
@RequestMapping("/organization/settings")
public class OrganizationSettingsController {
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    @Resource
    private IntegrationConfigService integrationConfigService;
    @Resource
    private DataEaseService dataEaseService;
    @Resource
    private DataEaseSyncService dataEaseSyncService;
    @Resource
    private OrganizationConfigService organizationConfigService;

    //获取邮件设置
    @GetMapping("/email")
    @Operation(summary = "获取邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public EmailDTO getEmail() {
        return organizationConfigService.getEmail(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/email/edit")
    @Operation(summary = "编辑邮件设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void editEmail(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.editEmail(emailDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/email/test")
    @Operation(summary = "系统设置-系统-系统参数-基本设置-邮件设置-测试连接")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public void verifyEmailConnection(@Validated @RequestBody EmailDTO emailDTO) {
        organizationConfigService.verifyEmailConnection(emailDTO);
    }

    //获取同步组织设置
    @GetMapping("/third-party")
    @Operation(summary = "获取三方设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public List<ThirdConfigurationDTO> getThirdConfig() {
        return integrationConfigService.getThirdConfig(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/switch-third-party")
    @Operation(summary = "切换三方设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void switchThirdPartySetting(@RequestParam String type) {
        integrationConfigService.switchThirdPartySetting(type, OrganizationContext.getOrganizationId());
    }

    //获取最新的三方同步标识
    @GetMapping("/third-party/sync/resource")
    @Operation(summary = "获取最新的三方同步来源")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public String getLatestSyncResource() {
        return integrationConfigService.getLatestSyncResource(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/third-party/edit")
    @Operation(summary = "编辑三方设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void editThirdConfig(@Validated @RequestBody ThirdConfigurationDTO thirdConfigurationDTO) {
        integrationConfigService.editThirdConfig(thirdConfigurationDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping(value = "/third-party/test")
    @Operation(summary = "校验配置是否链接成功")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public boolean validate(@RequestBody ThirdConfigurationDTO thirdConfigurationDTO) {
        return integrationConfigService.testConnection(thirdConfigurationDTO, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping(value = "/de-token")
    @Operation(summary = "生成DE-Token")
    @RequiresPermissions(PermissionConstants.DASHBOARD_READ)
    public DeAuthDTO generateToken(@RequestParam(required = false) boolean isModule) {
        return dataEaseService.getEmbeddedDeToken(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), isModule);
    }

    @GetMapping(value = "/de/sync")
    @Operation(summary = "同步DE数据")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void syncDataEase() {
        dataEaseSyncService.syncDataEase(OrganizationContext.getOrganizationId());
    }

    @PostMapping(value = "/de/org/list")
    @Operation(summary = "获取de组织列表")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public List<OptionDTO> getDeOrgList(@RequestBody ThirdConfigurationDTO thirdConfigurationDTO) {
        return dataEaseSyncService.getDeOrgList(thirdConfigurationDTO);
    }

    @GetMapping("/third-party/get/{type}")
    @Operation(summary = "根据类型获取开启的三方扫码设置")
    public ThirdConfigurationDTO getThirdConfigByType(@PathVariable String type) {
        String organizationId = Optional.ofNullable(OrganizationContext.getOrganizationId()).orElse(DEFAULT_ORGANIZATION_ID);
        return integrationConfigService.getThirdConfigForPublic(type, organizationId);
    }

    @GetMapping("/third-party/types")
    @Operation(summary = "获取三方应用扫码类型集合")
    public List<OptionDTO> getThirdTypeList() {
        String organizationId = Optional.ofNullable(OrganizationContext.getOrganizationId()).orElse(DEFAULT_ORGANIZATION_ID);
        return integrationConfigService.getThirdTypeList(organizationId);
    }

}