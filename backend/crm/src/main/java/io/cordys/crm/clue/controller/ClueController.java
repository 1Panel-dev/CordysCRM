package io.cordys.crm.clue.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.service.DataScopeService;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.dto.request.*;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "线索")
@RequestMapping("/clue")
public class ClueController {
    @Resource
    private ClueService clueService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索列表")
    public Pager<List<ClueListResponse>> list(@Validated @RequestBody CluePageRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), request.getSearchType());
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, clueService.list(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), deptDataPermission));
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索详情")
    public ClueGetResponse get(@PathVariable String id) {
        return clueService.get(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_ADD)
    @Operation(summary = "添加线索")
    public Clue add(@Validated @RequestBody ClueAddRequest request) {
        return clueService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索")
    public Clue update(@Validated @RequestBody ClueUpdateRequest request) {
        return clueService.update(request, SessionUtils.getUserId());
    }

    @PostMapping("/status/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索状态")
    public void updateStatus(@Validated @RequestBody ClueStatusUpdateRequest request) {
        clueService.updateStatus(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_DELETE)
    @Operation(summary = "删除线索")
    public void delete(@PathVariable String id) {
        clueService.delete(id);
    }

    @PostMapping("/batch/transfer")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "批量转移线索")
    public void batchTransfer(@RequestBody ClueBatchTransferRequest request) {
        clueService.batchTransfer(request);
    }

    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除线索")
    public void batchDelete(@RequestBody @NotEmpty List<String> ids) {
        clueService.batchDelete(ids);
    }

    @PostMapping("/batch/to-pool")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "批量移入线索池")
    public void batchToPool(@RequestBody @NotEmpty List<String> ids) {
        clueService.batchToPool(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
