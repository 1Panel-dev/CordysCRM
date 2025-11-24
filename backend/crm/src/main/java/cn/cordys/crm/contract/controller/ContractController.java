package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.ContractAddRequest;
import cn.cordys.crm.contract.dto.request.ContractUpdateRequest;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "合同")
@RestController
@RequestMapping("/contract")
public class ContractController {
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ContractService contractService;


    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.CONTRACT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_ADD)
    @Operation(summary = "创建")
    public Contract add(@Validated @RequestBody ContractAddRequest request) {
        return contractService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_UPDATE)
    @Operation(summary = "更新")
    public Contract update(@Validated @RequestBody ContractUpdateRequest request) {
        return contractService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        contractService.delete(id);
    }



}
