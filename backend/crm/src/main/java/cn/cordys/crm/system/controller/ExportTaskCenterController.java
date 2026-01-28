package cn.cordys.crm.system.controller;

import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import cn.cordys.crm.system.service.ExportTaskCenterService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author song-cc-rock
 */
@RestController
@RequestMapping("/export/center")
@Tag(name = "导出中心")
public class ExportTaskCenterController {

  @Resource private ExportTaskCenterService exportTaskCenterService;

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

  @GetMapping("/download/{taskId}")
  @Operation(summary = "下载")
  public ResponseEntity<org.springframework.core.io.Resource> download(
      @PathVariable("taskId") String taskId) {
    return exportTaskCenterService.download(taskId);
  }
}
