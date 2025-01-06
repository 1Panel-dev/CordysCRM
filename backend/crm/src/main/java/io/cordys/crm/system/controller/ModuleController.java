package io.cordys.crm.system.controller;

import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.response.ModuleDTO;
import io.cordys.crm.system.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/module")
@Tag(name = "模块设置")
public class ModuleController {

	@Resource
	private ModuleService moduleService;

	@PostMapping("/list")
	@Operation(summary = "获取模块设置列表")
	public List<ModuleDTO> getModuleList(@Validated @RequestBody ModuleRequest request) {
		return moduleService.getModuleList(request);
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "单个模块开启或关闭")
	public void switchModule(@PathVariable String id) {
		moduleService.switchModule(id);
	}
}
