package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.dto.request.ApprovalProcessedPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalTodoPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.service.ApprovalTodoService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/approval-todo")
@Tag(name = "审核代办")
public class ApprovalTodoController {

    @Resource
    private ApprovalTodoService approvalTodoService;

    @PostMapping("/pending/page")
    @Operation(summary = "审核代办-当前用户待审核资源分页")
    public Pager<List<ApprovalTodoItemResponse>> todo(@Validated @RequestBody ApprovalTodoPageRequest request) {
        return approvalTodoService.getTodoPage(request, SessionUtils.getUserId());
    }

    @PostMapping("/processed/page")
    @Operation(summary = "审核代办-当前用户已处理审批分页")
    public Pager<List<ApprovalTodoItemResponse>> processedPage(@Validated @RequestBody ApprovalProcessedPageRequest request) {
        return approvalTodoService.getProcessedPage(request, SessionUtils.getUserId());
    }
}
