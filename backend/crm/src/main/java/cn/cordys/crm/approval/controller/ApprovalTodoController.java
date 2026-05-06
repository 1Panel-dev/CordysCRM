package cn.cordys.crm.approval.controller;

import cn.cordys.crm.approval.dto.response.ApprovalTodoResponse;
import cn.cordys.crm.approval.service.ApprovalTodoService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/approval-todo")
@Tag(name = "审核代办")
public class ApprovalTodoController {

    @Resource
    private ApprovalTodoService approvalTodoService;

    @GetMapping("/list")
    @Operation(summary = "审核代办-当前用户待审核资源")
    public ApprovalTodoResponse todo() {
        return approvalTodoService.getTodoList(SessionUtils.getUserId());
    }
}
