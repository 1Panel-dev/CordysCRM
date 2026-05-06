package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.stage.StageAddRequest;
import cn.cordys.common.dto.stage.StageRollBackRequest;
import cn.cordys.common.dto.stage.StageUpdateRequest;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.dto.response.ContractStageConfigListResponse;
import cn.cordys.crm.contract.service.ContractStageService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同状态流设置")
@RestController
@RequestMapping("/contract/stage")
public class ContractStageController {

    @Resource
    private ContractStageService contractStageService;

    @GetMapping("/get")
    @Operation(summary = "合同状态配置列表")
    public ContractStageConfigListResponse getStageConfigList() {
        return contractStageService.getStageConfigList(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @Operation(summary = "添加合同状态流")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public String add(@RequestBody StageAddRequest request) {
        return contractStageService.addStageConfig(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "删除合同状态流")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void delete(@PathVariable("id") @Validated String id) {
        contractStageService.delete(id, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update-rollback")
    @Operation(summary = "合同状态流回退设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody StageRollBackRequest request) {
        contractStageService.updateRollBack(request, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新合同阶段配置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody StageUpdateRequest request) {
        contractStageService.update(request, SessionUtils.getUserId());
    }


    @PostMapping("/sort")
    @Operation(summary = "合同阶段排序")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public void sort(@RequestBody List<String> ids) {
        contractStageService.sort(ids, OrganizationContext.getOrganizationId());
    }
}

