package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.crm.approval.dto.request.ApprovalRejectRequest;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.service.ApprovalAddSignService;
import cn.cordys.crm.approval.service.ApprovalRejectService;
import cn.cordys.crm.approval.service.ApprovalReturnBackService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审批流按钮操作类")
@RestController
@RequestMapping("/approval-operation")
public class ApprovalOperationController {

    @Resource
    private ApprovalAddSignService approvalAddSignService;
    @Resource
    private ApprovalReturnBackService approvalReturnBackService;
    @Resource
    private ApprovalRejectService approvalRejectService;


    @PostMapping("/add")
    @Operation(summary = "新建加签")
    public void add(@Validated @RequestBody ApprovalAddSignRequest request) {
        approvalAddSignService.addSign(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/back")
    @Operation(summary = "回退")
    public void back(@Validated @RequestBody ApprovalReturnBackRequest request) {
        approvalReturnBackService.back(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/reject")
    @Operation(summary = "驳回")
    public void reject(@Validated @RequestBody ApprovalRejectRequest request) {
        approvalRejectService.reject(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
