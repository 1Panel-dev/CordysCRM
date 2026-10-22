package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalCommentAddRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentUpdateRequest;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.service.ApprovalCommentService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审批评论")
@RestController
@RequestMapping("/approval-comment")
public class ApprovalCommentController {

    @Resource
    private ApprovalCommentService approvalCommentService;

    @PostMapping("/page")
    @Operation(summary = "分页查询审批评论")
    public Pager<List<ApprovalCommentResponse>> page(@Validated @RequestBody ApprovalCommentPageRequest request) {
        return approvalCommentService.page(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "添加审批评论")
    public ApprovalCommentResponse add(@Validated @RequestBody ApprovalCommentAddRequest request) {
        return approvalCommentService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "编辑审批评论")
    public ApprovalCommentResponse update(@Validated @RequestBody ApprovalCommentUpdateRequest request) {
        return approvalCommentService.update(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除审批评论")
    public void delete(@PathVariable String id) {
        approvalCommentService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
