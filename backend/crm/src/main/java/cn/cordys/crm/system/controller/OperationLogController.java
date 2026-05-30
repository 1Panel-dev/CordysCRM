package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.OperationLogRequest;
import cn.cordys.crm.system.dto.response.OperationLogDetailResponse;
import cn.cordys.crm.system.dto.response.OperationLogResponse;
import cn.cordys.crm.system.service.SysOperationLogService;
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
@RequestMapping("/operation/log")
@Tag(name = "日志")
public class OperationLogController {

    @Resource
    private SysOperationLogService sysOperationLogService;

    @PostMapping("/list")
    @Operation(summary = "系统管理-操作日志-列表查询")
    @RequiresPermissions(PermissionConstants.OPERATION_LOG_READ)
    public Pager<List<OperationLogResponse>> list(@Validated @RequestBody OperationLogRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, sysOperationLogService.list(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "系统管理-操作日志-详情")
    @RequiresPermissions(PermissionConstants.OPERATION_LOG_READ)
    public OperationLogDetailResponse logDetail(@PathVariable String id) {
        return sysOperationLogService.getLogDetail(id, OrganizationContext.getOrganizationId());
    }
}