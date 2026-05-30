package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.LoginLogRequest;
import cn.cordys.crm.system.dto.response.LoginLogListResponse;
import cn.cordys.crm.system.service.SysLoginLogService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/login/log")
@Tag(name = "登入日志")
public class LoginLogController {

    @Resource
    private SysLoginLogService sysLoginLogService;

    @PostMapping("/list")
    @Operation(summary = "系统管理-登录日志-列表查询")
    @RequiresPermissions(PermissionConstants.OPERATION_LOG_READ)
    public Pager<List<LoginLogListResponse>> loginList(@Validated @RequestBody LoginLogRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, sysLoginLogService.list(request, OrganizationContext.getOrganizationId()));
    }
}