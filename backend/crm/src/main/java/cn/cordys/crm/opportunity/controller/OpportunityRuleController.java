package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import cn.cordys.crm.opportunity.dto.request.OpportunityRuleAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityRuleUpdateRequest;
import cn.cordys.crm.opportunity.service.OpportunityRuleService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/opportunity-rule")
@Tag(name = "商机规则")
public class OpportunityRuleController {

	@Resource
	private OpportunityRuleService opportunityRuleService;

	@PostMapping("/page")
	@Operation(summary = "分页获取商机规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public Pager<List<OpportunityRuleDTO>> page(@Validated @RequestBody BasePageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		return PageUtils.setPageInfo(page, opportunityRuleService.page(request, OrganizationContext.getOrganizationId()));
	}

	@PostMapping("/add")
	@Operation(summary = "添加商机规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void save(@Validated @RequestBody OpportunityRuleAddRequest request) {
		opportunityRuleService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/update")
	@Operation(summary = "修改商机规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void update(@Validated @RequestBody OpportunityRuleUpdateRequest request) {
		opportunityRuleService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除商机规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void delete(@PathVariable String id) {
		opportunityRuleService.delete(id, SessionUtils.getUserId());
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "启用/禁用商机规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void switchStatus(@PathVariable String id) {
		opportunityRuleService.switchStatus(id, SessionUtils.getUserId());
	}
}
