package io.cordys.crm.clue.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.utils.ConditionFilterUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.clue.dto.request.ClueExportRequest;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.request.PoolClueAssignRequest;
import io.cordys.crm.clue.dto.request.PoolCluePickRequest;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.service.CluePoolExportService;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.clue.service.PoolClueService;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "线索池客户")
@RestController
@RequestMapping("/pool/clue")
public class PoolClueController {

	@Resource
	private PoolClueService poolClueService;
	@Resource
	private ClueService clueService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private CluePoolExportService cluePoolExportService;

	@GetMapping("/options")
	@Operation(summary = "获取当前用户线索池选项")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_READ})
	public List<CluePoolDTO> getPoolOptions() {
		return poolClueService.getPoolOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/page")
	@Operation(summary = "线索列表")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_READ})
	public PagerWithOption<List<ClueListResponse>> list(@Validated @RequestBody CluePageRequest request) {
		ConditionFilterUtils.parseCondition(request);
		return clueService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), null);
	}

	@PostMapping("/pick")
	@Operation(summary = "领取线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_PICK})
	public void pick(@Validated @RequestBody PoolCluePickRequest request) {
		poolClueService.pick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/assign")
	@Operation(summary = "分配线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_ASSIGN})
	public void assign(@Validated @RequestBody PoolClueAssignRequest request) {
		poolClueService.assign(request.getClueId(), request.getAssignUserId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_DELETE})
	public void delete(@PathVariable String id) {
		poolClueService.delete(id);
	}

	@GetMapping("/get/{id}")
	@RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_POOL_READ)
	@Operation(summary = "线索详情")
	public ClueGetResponse get(@PathVariable String id) {
		return clueService.get(id, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-pick")
	@Operation(summary = "批量领取线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_PICK})
	public void batchPick(@Validated @RequestBody PoolBatchPickRequest request) {
		poolClueService.batchPick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-assign")
	@Operation(summary = "批量分配线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_ASSIGN})
	public void batchAssign(@Validated @RequestBody PoolBatchAssignRequest request) {
		poolClueService.batchAssign(request, request.getAssignUserId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@PostMapping("/batch-delete")
	@Operation(summary = "批量删除线索")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_POOL_DELETE})
	public void batchDelete(@RequestBody @NotEmpty List<String> ids) {
		poolClueService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

    @PostMapping("/export-all")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_POOL_EXPORT)
    @Operation(summary = "导出全部")
    public String exportAll(@Validated @RequestBody ClueExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return cluePoolExportService.exportCrossPage(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, LocaleContextHolder.getLocale());
    }

    @PostMapping("/export-select")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_POOL_EXPORT)
    @Operation(summary = "导出选中")
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return cluePoolExportService.exportSelect(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }
}
