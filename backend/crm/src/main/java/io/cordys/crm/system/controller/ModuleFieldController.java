package io.cordys.crm.system.controller;

import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleFieldSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.service.ModuleFieldService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/module/field")
@Tag(name = "模块-字段设置")
public class ModuleFieldController {

	@Resource
	private ModuleFieldService moduleFieldService;

	@PostMapping("/save")
	@Operation(summary = "保存字段列表")
	public List<ModuleFieldDTO> save(@Validated @RequestBody ModuleFieldSaveRequest request) {
		return moduleFieldService.save(request, SessionUtils.getUserId());
	}

	@PostMapping("/list")
	@Operation(summary = "获取模块字段列表")
	public List<ModuleFieldDTO> getFieldList(@Validated @RequestBody ModuleFieldRequest request) {
		return moduleFieldService.getFieldList(request);
	}
}
