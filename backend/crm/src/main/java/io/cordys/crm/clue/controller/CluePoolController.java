package io.cordys.crm.clue.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.clue.dto.request.CluePoolAddRequest;
import io.cordys.crm.clue.dto.request.CluePoolUpdateRequest;
import io.cordys.crm.clue.service.CluePoolService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clue-pool")
@Tag(name = "线索池设置")
public class CluePoolController {

	@Resource
	private CluePoolService cluePoolService;

	@PostMapping("/page")
	@Operation(summary = "分页获取线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public Pager<List<CluePoolDTO>> page(@Validated @RequestBody BasePageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		return PageUtils.setPageInfo(page, cluePoolService.page(request));
	}

	@PostMapping("/add")
	@Operation(summary = "新增线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void add(@Validated @RequestBody CluePoolAddRequest request) {
		cluePoolService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/update")
	@Operation(summary = "编辑线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void update(@Validated @RequestBody CluePoolUpdateRequest request) {
		cluePoolService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/quick-update")
	@Operation(summary = "编辑线索池")
	public void quickUpdate(@Validated @RequestBody CluePoolUpdateRequest request) {
		cluePoolService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/no-pick/{id}")
	@Operation(summary = "线索池是否存在未领取线索")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public boolean checkNoPick(@PathVariable String id) {
		return cluePoolService.checkNoPick(id);
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void delete(@PathVariable String id) {
		cluePoolService.delete(id);
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "启用/禁用线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void switchStatus(@PathVariable String id) {
		cluePoolService.switchStatus(id, SessionUtils.getUserId());
	}
}
