package io.cordys.crm.system.controller;


import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.dto.request.MessageTaskBatchRequest;
import io.cordys.crm.system.dto.request.MessageTaskRequest;
import io.cordys.crm.system.dto.response.MessageTaskDTO;
import io.cordys.crm.system.service.MessageTaskService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息设置")
@RestController
@RequestMapping("/message/task/")
public class MessageTaskController {
    @Resource
    private MessageTaskService messageTaskService;

    @PostMapping("save")
    @Operation(summary = "项目管理-消息管理-消息设置-保存消息设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public MessageTask saveMessage(@Validated({Created.class, Updated.class}) @RequestBody MessageTaskRequest messageTaskRequest) {
        return messageTaskService.saveMessageTask(messageTaskRequest, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("batch/save")
    @Operation(summary = "项目管理-消息管理-消息设置-批量编辑")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public void batchSaveMessage(@Validated({Created.class, Updated.class}) @RequestBody MessageTaskBatchRequest messageTaskBatchRequest) {
        messageTaskService.batchSaveMessageTask(messageTaskBatchRequest, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("get")
    @Operation(summary = "项目管理-消息管理-消息设置-获取消息设置")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public List<MessageTaskDTO> getMessageList() {
        return messageTaskService.getMessageList(OrganizationContext.getOrganizationId());
    }


}

