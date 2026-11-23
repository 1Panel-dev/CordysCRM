package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.message.MessageAddRequest;
import cn.cordys.crm.system.dto.message.MessageBatchRequest;
import cn.cordys.crm.system.dto.message.MessageRequest;
import cn.cordys.crm.system.dto.message.MessageTaskCountDTO;
import cn.cordys.crm.system.dto.message.MessageTaskResponse;
import cn.cordys.crm.system.dto.message.MessageUpdateRequest;
import cn.cordys.crm.system.service.MessageNotificationService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "消息设置")
@RestController
@RequestMapping("/message/task/")
public class MessageTaskController {
    @Resource
    private MessageNotificationService messageNotificationService;

    //获取系统类型以及数量
    @GetMapping("/sys")
    @Operation(summary = "消息设置-获取系统类型以及数量")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public List<MessageTaskCountDTO> getSysTaskTypeCount() {
        return messageNotificationService.getSysTaskTypeCount(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/custom-form")
    @Operation(summary = "消息设置-获取自定义类型以及数量")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public List<MessageTaskCountDTO> getCustomTaskTypeCount() {
        return messageNotificationService.getCustomTaskTypeCount(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @Operation(summary = "列表数据(按通知类型获取)")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public Pager<List<MessageTaskResponse>> messageList(@Validated @RequestBody MessageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, messageNotificationService.getMessageList(request));
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_ADD)
    @Operation(summary = "添加")
    public void add(@Validated @RequestBody MessageAddRequest request) {
        messageNotificationService.add(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    @Operation(summary = "更新")
    public void update(@Validated @RequestBody MessageUpdateRequest request) {
        messageNotificationService.update(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable String id) {
        messageNotificationService.delete(id);
    }

    @GetMapping("/enable/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    @Operation(summary = "启用/禁用")
    public void enable(@PathVariable String id) {
        messageNotificationService.enable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(value = PermissionConstants.SYSTEM_NOTICE_UPDATE)
    @Operation(summary = "批量更新接收方式")
    public void batchUpdate(@Validated @RequestBody MessageBatchRequest request) {
        messageNotificationService.batchUpdate(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }
}
