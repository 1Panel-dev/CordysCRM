package io.cordys.crm.opportunity.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机")
@RestController
@RequestMapping("/opportunity")
public class OpportunityController {

    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ModuleFormService moduleFormService;


    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormService.getConfig(FormKey.BUSINESS.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机列表")
    public Pager<List<OpportunityListResponse>> list(@Validated @RequestBody OpportunityPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, opportunityService.list(request, OrganizationContext.getOrganizationId()));
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)
    @Operation(summary = "添加商机")
    public Opportunity add(@Validated @RequestBody OpportunityAddRequest request) {
        return opportunityService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "更新商机")
    public Opportunity update(@Validated @RequestBody OpportunityUpdateRequest request) {
        return opportunityService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "删除商机")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE)
    public void deleteOpportunity(@PathVariable String id) {
        opportunityService.delete(id);
    }

    @PostMapping("/transfer")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "转移商机")
    public void transfer(@RequestBody OpportunityTransferRequest request) {
        opportunityService.transfer(request);
    }


    @PostMapping("/batch/transfer")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "批量转移商机")
    public void batchTransfer(@RequestBody OpportunityTransferRequest request) {
        opportunityService.transfer(request);
    }


    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除商机")
    public void delete(@RequestBody @NotEmpty List<String> ids) {
        opportunityService.batchDelete(ids);
    }
}
