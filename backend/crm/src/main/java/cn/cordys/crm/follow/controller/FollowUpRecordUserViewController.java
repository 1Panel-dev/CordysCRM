package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.UserViewResourceType;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import cn.cordys.crm.system.dto.response.UserViewListResponse;
import cn.cordys.crm.system.dto.response.UserViewResponse;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "记录视图")
@RestController
@RequestMapping("/follow/record/view")
public class FollowUpRecordUserViewController {

	@Resource
	private UserViewService userViewService;


	@PostMapping("/add")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "添加记录视图")
	public UserView add(@Validated @RequestBody UserViewAddRequest request) {
		return userViewService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), UserViewResourceType.FOLLOW_RECORD.name());
	}


	@PostMapping("/update")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "编辑记录视图")
	public UserView update(@Validated @RequestBody UserViewUpdateRequest request) {
		return userViewService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}


	@GetMapping("/delete/{id}")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "删除记录视图")
	public void delete(@PathVariable String id) {
		userViewService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/detail/{id}")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "记录视图详情")
	public UserViewResponse viewDetail(@PathVariable String id) {
		return userViewService.getViewDetail(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), FormKey.FOLLOW_RECORD.getKey());
	}


	@GetMapping("/list")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "记录视图列表")
	public List<UserViewListResponse> queryList() {
		return userViewService.list(UserViewResourceType.FOLLOW_RECORD.name(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}


	@GetMapping("/fixed/{id}")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "记录视图固定/取消固定")
	public void fixed(@PathVariable String id) {
		userViewService.fixed(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/edit/pos")
	@Operation(summary = "记录视图-拖拽排序")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	public void editPos(@Validated @RequestBody PosRequest request) {
		userViewService.editPos(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), UserViewResourceType.FOLLOW_RECORD.name());
	}


	@GetMapping("/enable/{id}")
	@RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
	@Operation(summary = "记录视图-启用/禁用")
	public void enable(@PathVariable String id) {
		userViewService.enable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}
}
