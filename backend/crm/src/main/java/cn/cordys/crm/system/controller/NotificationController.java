package cn.cordys.crm.system.controller;


import cn.cordys.common.dto.OptionCountDTO;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.NotificationRequest;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.service.NotificationService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息中心")
@RestController
@RequestMapping(value = "/notification")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @PostMapping(value = "/list/all/page")
    @Operation(summary = "消息中心-获取消息中心所有消息列表")
    public Pager<List<NotificationDTO>> listNotification(@Validated @RequestBody NotificationRequest notificationRequest) {
        Page<Object> page = PageHelper.startPage(notificationRequest.getCurrent(), notificationRequest.getPageSize(), true);
        return PageUtils.setPageInfo(page, notificationService.listNotification(notificationRequest, SessionUtils.getUserId(), OrganizationContext.getOrganizationId()));
    }


    @GetMapping(value = "/last/list")
    @Operation(summary = "消息中心-获取消息中未读的最新部分消息")
    public List<NotificationDTO> listLastNotification() {
        return notificationService.listLastNotification(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping(value = "/last/announcement/list")
    @Operation(summary = "消息中心-获取用户未读的公告列表")
    public List<NotificationDTO> listLastAnnouncement() {
        return notificationService.listLastAnnouncement(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping(value = "/read/{id}")
    @Operation(summary = "消息中心-将消息设置为已读")
    public Integer read(@PathVariable String id) {
        return notificationService.read(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping(value = "/read/all")
    @Operation(summary = "消息中心-将消息中心所有信息设置为已读消息")
    public Integer readAll() {
        return notificationService.readAll(OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping(value = "/count")
    @Operation(summary = "消息中心-获取消息中心消息具体类型具体状态的数量")
    public List<OptionCountDTO> countNotification(@RequestBody NotificationRequest notificationRequest) {
        return notificationService.countNotification(notificationRequest, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
