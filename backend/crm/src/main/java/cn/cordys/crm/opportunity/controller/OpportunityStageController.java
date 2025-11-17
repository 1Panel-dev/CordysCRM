package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.request.OpportunityStageAddRequest;
import cn.cordys.crm.opportunity.dto.request.StageRollBackRequest;
import cn.cordys.crm.opportunity.dto.request.StageUpdateRequest;
import cn.cordys.crm.opportunity.dto.response.StageConfigListResponse;
import cn.cordys.crm.opportunity.service.OpportunityStageService;
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


    @GetMapping("/get")
    @Operation(summary = "商机阶段配置列表")
    public StageConfigListResponse getStageConfigList() {
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

}
