package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.system.dto.request.OperationLogRequest;
import io.cordys.crm.system.dto.response.OperationLogResponse;
import io.cordys.crm.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operation/log")
@Tag(name = "日志")
public class OperationLogController {

    @Resource
    private SysOperationLogService sysOperationLogService;

    @PostMapping("/list")
    @Operation(summary = "系统管理-操作日志-列表查询")
    //@RequiresPermissions(PermissionConstants.OPERATION_LOG_READ) todo add permission
    public Pager<List<OperationLogResponse>> list(@Validated @RequestBody OperationLogRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, sysOperationLogService.list(request));
    }


    @PostMapping("/login/list")
    @Operation(summary = "系统管理-登录日志-列表查询")
    //@RequiresPermissions(PermissionConstants.OPERATION_LOG_READ) todo add permission
    public Pager<List<OperationLogResponse>> loginList(@Validated @RequestBody OperationLogRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, sysOperationLogService.loginList(request));
    }


    @GetMapping("/detail/{id}")
    @Operation(summary = "系统管理-操作日志-详情")
    //@RequiresPermissions(PermissionConstants.OPERATION_LOG_READ) todo add permission
    public List<JsonDifferenceDTO> logDetail(@PathVariable String id) {
        return sysOperationLogService.getLogDetail(id);
    }
}