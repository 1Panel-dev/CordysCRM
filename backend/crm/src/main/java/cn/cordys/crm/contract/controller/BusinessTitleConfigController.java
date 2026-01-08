package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.BusinessTitleConfig;
import cn.cordys.crm.contract.service.BusinessTitleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "工商抬头配置")
@RestController
@RequestMapping("/business-title/config")
public class BusinessTitleConfigController {

    @Resource
    private BusinessTitleConfigService businessTitleConfigService;


    @GetMapping("/get")
    @Operation(summary = "获取工商抬头配置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_READ})
    public List<BusinessTitleConfig> getStageConfigList() {
        return businessTitleConfigService.getConfigs(OrganizationContext.getOrganizationId());
    }


    @GetMapping("/switch/{id}")
    @Operation(summary = "必填/非必填设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void switchRequired(@PathVariable String id) {
        businessTitleConfigService.switchRequired(id);
    }
}
