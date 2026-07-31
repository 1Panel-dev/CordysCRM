package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.dto.request.ApprovalTodoPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.service.ApprovalTodoService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approval-todo")
@Tag(name = "审核代办")
public class ApprovalTodoController {

    @Resource
    private ApprovalTodoService approvalTodoService;

    @PostMapping("/pending/page")
    @Operation(summary = "审核代办-当前用户待审核资源分页", description = "返回值：Pager<List<ApprovalTodoItemResponse>>")
    public Pager<List<ApprovalTodoItemResponse>> todo(@Validated @RequestBody ApprovalTodoPageRequest request) {
        return approvalTodoService.getTodoPage(request, SessionUtils.getUserId());
    }

    @PostMapping("/processed/page")
    @Operation(summary = "审核代办-当前用户已处理审批分页", description = "返回值：Pager<List<ApprovalTodoItemResponse>>")
    public Pager<List<ApprovalTodoItemResponse>> processedPage(@Validated @RequestBody ApprovalTodoPageRequest request) {
        return approvalTodoService.getProcessedPage(request, SessionUtils.getUserId());
    }

    @PostMapping("/initiated/page")
    @Operation(summary = "审核代办-我发起的审批分页", description = "返回值：Pager<List<ApprovalTodoItemResponse>>")
    public Pager<List<ApprovalTodoItemResponse>> initiatedPage(@Validated @RequestBody ApprovalTodoPageRequest request) {
        return approvalTodoService.getInitiatedPage(request, SessionUtils.getUserId());
    }

    @PostMapping("/cc/page")
    @Operation(summary = "审核代办-抄送我的审批分页", description = "返回值：Pager<List<ApprovalTodoItemResponse>>")
    public Pager<List<ApprovalTodoItemResponse>> ccPage(@Validated @RequestBody ApprovalTodoPageRequest request) {
        return approvalTodoService.getCcPage(request, SessionUtils.getUserId());
    }

    @GetMapping("/pending/count")
    @Operation(summary = "审核代办-待我审批统计")
    public ApprovalTodoCountResponse pendingCount() {
        return approvalTodoService.getPendingCount(SessionUtils.getUserId());
    }
}
