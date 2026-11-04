package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalActionBatchRequest;
import cn.cordys.crm.approval.dto.request.ApprovalActionRequest;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.dto.request.ApprovalRevokeRequest;
import cn.cordys.crm.approval.dto.response.ApprovalResubmitNodeResponse;
import cn.cordys.crm.approval.service.ApprovalActionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "审批操作")
@RestController
@RequestMapping("/approval-action")
public class ApprovalActionController {

    @Resource
    private ApprovalActionService approvalActionService;

    @GetMapping("/resubmit-nodes/{taskId}")
    @Operation(summary = "查询本轮可指定的重审节点", description = "首项 nodeName 为本轮审批实例提交人的姓名，nodeNumber 为空字符串；选择该项时 resubmitStartNodeNumber 传空字符串，表示整单重审")
    public List<ApprovalResubmitNodeResponse> getResubmitNodes(
            @Parameter(description = "当前审批任务ID", required = true)
            @PathVariable String taskId) {
        return approvalActionService.getResubmitNodes(taskId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/sign")
    @Operation(summary = "加签")
    public void add(@Validated @RequestBody ApprovalAddSignRequest request) {
        approvalActionService.sign(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/back")
    @Operation(summary = "退回")
    public void back(@Validated @RequestBody ApprovalReturnBackRequest request) {
        approvalActionService.back(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤回")
    public void revoke(@Validated @RequestBody ApprovalRevokeRequest request) {
        approvalActionService.revoke(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/approve")
    @Operation(summary = "同意")
    public void approve(@Validated @RequestBody ApprovalActionRequest request) {
        approvalActionService.approve(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回")
    public void reject(@Validated @RequestBody ApprovalActionRequest request) {
        approvalActionService.reject(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量同意")
    public void batchApprove(@Validated @RequestBody ApprovalActionBatchRequest request) {
        approvalActionService.batchApprove(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch-reject")
    @Operation(summary = "批量驳回")
    public void batchReject(@Validated @RequestBody ApprovalActionBatchRequest request) {
        approvalActionService.batchReject(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
