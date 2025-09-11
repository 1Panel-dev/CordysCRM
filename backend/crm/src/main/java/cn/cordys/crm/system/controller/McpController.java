package cn.cordys.crm.system.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.field.base.SimpleField;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP三方接口")
public class McpController {

	@Resource
	private ModuleFormCacheService moduleFormCacheService;

	@GetMapping("/form/config/{formKey}")
	@Operation(summary = "获取表单配置")
	public List<SimpleField> getMcpField(@PathVariable String formKey) {
		// 可以定制化一些字段的返回, 目前是全量返回
		return moduleFormCacheService.getMcpFields(formKey, OrganizationContext.getOrganizationId());
	}
}
