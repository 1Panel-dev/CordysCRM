package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.stage.StageAdvancedConfigRequest;
import cn.cordys.common.dto.stage.StageConfigsResponse;
import cn.cordys.common.dto.stage.StageRollBackRequest;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.request.OpportunityStageAddRequest;
import cn.cordys.crm.opportunity.dto.request.StageUpdateRequest;
import cn.cordys.crm.opportunity.service.OpportunityStageService;
import cn.cordys.crm.system.service.StageAdvancedConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机阶段设置")
@RestController
@RequestMapping("/opportunity/stage")
public class OpportunityStageController {
    @Resource
    private OpportunityStageService opportunityStageService;
    @Resource
    private StageAdvancedConfigService stageAdvancedConfigService;


    @GetMapping("/get")
    @Operation(summary = "商机阶段配置列表")
    public StageConfigsResponse getStageConfigList() {
        return opportunityStageService.getStageConfigList(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @Operation(summary = "添加商机阶段")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public String add(@RequestBody OpportunityStageAddRequest request) {
        return opportunityStageService.addStageConfig(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "删除商机阶段")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void delete(@PathVariable("id") @Validated String id) {
        opportunityStageService.delete(id, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update-rollback")
    @Operation(summary = "商机阶段回退设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody StageRollBackRequest request) {
        opportunityStageService.updateRollBack(request, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新商机阶段配置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody StageUpdateRequest request) {
        opportunityStageService.update(request, SessionUtils.getUserId());
    }


    @PostMapping("/sort")
    @Operation(summary = "商机阶段排序")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public void sort(@RequestBody List<String> ids) {
        opportunityStageService.sort(ids, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/circulation-type/{type}")
    @Operation(summary = "基础/高级流转切换")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void circulationType(@PathVariable String type) {
        stageAdvancedConfigService.switchType(type, FormKey.OPPORTUNITY.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/advanced/config")
    @Operation(summary = "商机流转配置保存")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void advancedConfigAdd(@RequestBody StageAdvancedConfigRequest request) {
        stageAdvancedConfigService.saveAdvancedConfig(request, FormKey.OPPORTUNITY.getKey(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

}
