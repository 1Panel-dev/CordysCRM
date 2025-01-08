package io.cordys.crm.lead.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolPageRequest;
import io.cordys.crm.lead.dto.request.LeadPoolSaveRequest;
import io.cordys.crm.lead.service.LeadPoolService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lead-pool")
@Tag(name = "线索池设置")
public class LeadPoolController {

	@Resource
	private LeadPoolService leadPoolService;

	@PostMapping("/page")
	@Operation(summary = "分页查询线索池")
	public Pager<List<LeadPoolDTO>> page(@Validated @RequestBody LeadPoolPageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
		return PageUtils.setPageInfo(page, leadPoolService.page(request));
	}

	@PostMapping("/add")
	@Operation(summary = "新增线索池")
	public void add(@Validated @RequestBody LeadPoolSaveRequest request) {
		leadPoolService.save(request, SessionUtils.getUserId());
	}

	@PostMapping("/update")
	@Operation(summary = "编辑线索池")
	public void update(@Validated @RequestBody LeadPoolSaveRequest request) {
		leadPoolService.save(request, SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索池")
	public void delete(@PathVariable String id) {
		leadPoolService.delete(id);
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "启用/禁用线索池")
	public void switchStatus(@PathVariable String id) {
		leadPoolService.switchStatus(id, SessionUtils.getUserId());
	}
}
