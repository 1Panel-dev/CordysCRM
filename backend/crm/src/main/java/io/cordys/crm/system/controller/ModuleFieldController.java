package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.domain.BaseModel;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleSourceDataRequest;
import io.cordys.crm.system.service.ModuleFieldService;
import io.cordys.crm.system.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/field")
@Tag(name = "字段管理")
public class ModuleFieldController {

	@Resource
	private ModuleService moduleService;
	@Resource
	private ModuleFieldService moduleFieldService;

	@PostMapping("/dept/tree")
	@Operation(summary = "获取部门树")
	public List<DeptUserTreeNode> getDeptTree(@Valid @RequestBody ModuleFieldRequest request) {
		moduleFieldService.checkFormField(request.getFormKey(), request.getFieldId(), OrganizationContext.getOrganizationId());
		return moduleFieldService.getDeptTree(OrganizationContext.getOrganizationId());
	}

	@PostMapping("/user/dept/tree")
	@Operation(summary = "获取部门用户树")
	public List<DeptUserTreeNode> getDeptUserTree(@Valid @RequestBody ModuleFieldRequest request) {
		moduleFieldService.checkFormField(request.getFormKey(), request.getFieldId(), OrganizationContext.getOrganizationId());
		return moduleService.getDeptUserTree(OrganizationContext.getOrganizationId());
	}

	@PostMapping("/source/data")
	@Operation(summary = "分页获取数据源数据")
	public Pager<List<? extends BaseModel>> getSourceData(@Valid @RequestBody ModuleSourceDataRequest request) {
		moduleFieldService.checkFormField(request.getFormKey(), request.getFieldId(), OrganizationContext.getOrganizationId());
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		return PageUtils.setPageInfo(page, moduleFieldService.getSourceData(request, OrganizationContext.getOrganizationId()));
	}
}
