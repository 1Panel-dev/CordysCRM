package io.cordys.crm.system.controller;

import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import io.cordys.crm.system.service.ExportTaskCenterService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author song-cc-rock
 */

@RestController
@RequestMapping("/export/center")
@Tag(name = "导出中心")
public class ExportTaskCenterController {

	@Resource
	private ExportTaskCenterService exportTaskCenterService;

	@PostMapping("/list")
	@Operation(summary = "查询导出任务列表")
	public List<ExportTask> queryList(@RequestBody ExportTaskCenterQueryRequest request) {
		return exportTaskCenterService.list(request, SessionUtils.getUserId());
	}

	@GetMapping("/cancel/{taskId}")
	@Operation(summary = "取消导出")
	public void cancel(@PathVariable("taskId") String taskId) {
		exportTaskCenterService.cancel(taskId);
	}

	@GetMapping("download/{taskId}")
	@Operation(summary = "下载")
	public void download(@PathVariable("taskId") String taskId, HttpServletResponse response) {
		exportTaskCenterService.download(taskId, response);
	}
}
