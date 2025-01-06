package io.cordys.crm.system.controller;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.dto.response.OptionDTO;
import io.cordys.crm.system.service.NotificationService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息中心")
@RestController
@RequestMapping(value = "notification")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @PostMapping(value = "/list/all/page")
    @Operation(summary = "消息中心-获取消息中心所有消息列表")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public Pager<List<NotificationDTO>> listNotification(@Validated @RequestBody NotificationRequest notificationRequest) {
        Page<Object> page = PageHelper.startPage(notificationRequest.getCurrent(), notificationRequest.getPageSize(), true);
        return PageUtils.setPageInfo(page, notificationService.listNotification(notificationRequest, SessionUtils.getUserId()));
    }

    @GetMapping(value = "/read/{id}")
    @Operation(summary = "消息中心-将消息设置为已读")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public Integer read(@PathVariable long id) {
        return notificationService.read(id, SessionUtils.getUserId());
    }

    @GetMapping(value = "/read/all")
    @Operation(summary = "消息中心-将消息中心所有信息设置为已读消息")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public Integer readAll(@RequestParam(value = "organizationId", required = false) String organizationId) {
        return notificationService.readAll(organizationId, SessionUtils.getUserId());
    }

    @GetMapping(value = "/un-read/{organizationId}")
    @Operation(summary = "消息中心-获取未读的消息")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public Integer getUnRead(@PathVariable(value = "organizationId") String organizationId) {
        return notificationService.getUnRead(organizationId, SessionUtils.getUserId());
    }

    @PostMapping(value = "/count")
    @Operation(summary = "消息中心-获取消息中心消息具体类型具体状态的数量")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public List<OptionDTO> countNotification(@RequestBody NotificationRequest notificationRequest) {
        return notificationService.countNotification(notificationRequest, SessionUtils.getUserId());
    }
}
