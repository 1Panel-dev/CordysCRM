package cn.cordys.crm.approval.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.response.ApprovalInstanceHistoryResponse;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.service.ApprovalCommentService;
import cn.cordys.crm.approval.service.ApprovalHistoryService;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源审批相关
 */
@RestController
@RequestMapping("/approval-resource")
@Tag(name = "审批资源")
public class ApprovalResourceController {

    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private ApprovalInstanceService approvalInstanceService;
    @Resource
    private ApprovalCommentService approvalCommentService;
    @Resource
    private ApprovalHistoryService approvalHistoryService;

    @PostMapping("/push")
    @Operation(summary = "提审")
    public void push(@RequestBody ApprovalResourceBaseParam param) {
        ApprovalPushParam approvalPushParam = ApprovalPushParam.builder()
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .resourceId(param.getResourceId())
                .formKey(param.getFormKey())
                .executeTimingEnum(ExecuteTimingEnum.CREATE)
                .build();
        approvalResourceService.push(approvalPushParam);
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销")
    public void revoke(@RequestBody ApprovalResourceBaseParam param) {
        approvalResourceService.revoke(param, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/simple-detail/{resourceId}")
    @Operation(summary = "列表详情")
    public ResourceApprovalResponse resourceDetail(@PathVariable String resourceId,
                                                   @RequestParam(required = false) String commentMentionId) {
        if (StringUtils.isNotBlank(commentMentionId)) {
            approvalCommentService.getMentionInstance(commentMentionId, resourceId, SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId());
        } else {
            approvalResourceService.checkViewPermission(resourceId);
        }
        return approvalResourceService.resourceDetail(resourceId);
    }

    @GetMapping("/detail/{resourceId}")
    @Operation(summary = "记录详情")
    public ApprovalInstanceDetail getRecordDetail(@PathVariable String resourceId,
                                                 @RequestParam(required = false) String commentMentionId) {
        String orgId = OrganizationContext.getOrganizationId();
        ApprovalInstanceDetail detail;
        if (StringUtils.isNotBlank(commentMentionId)) {
            ApprovalInstance instance = approvalCommentService.getMentionInstance(commentMentionId, resourceId,
                    SessionUtils.getUserId(), orgId);
            detail = approvalInstanceService.getApprovalInstanceDetail(instance, orgId);
        } else {
            approvalResourceService.checkViewPermission(resourceId);
            detail = approvalInstanceService.getLatestApprovalInstanceDetail(resourceId, orgId);
        }
        if (detail != null) {
            detail.setCommentable(
                    approvalCommentService.hasCommentPermission(detail.getId(), SessionUtils.getUserId(), orgId));
        }
        return detail;
    }

    @GetMapping("/instances/{resourceId}")
    @Operation(summary = "查询历史审批实例")
    public List<ApprovalInstanceHistoryResponse> getHistoryInstances(@PathVariable String resourceId) {
        return approvalHistoryService.getHistoryInstances(resourceId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/instance-detail/{instanceId}")
    @Operation(summary = "查询历史审批实例详情")
    public ApprovalInstanceDetail getHistoryInstanceDetail(@PathVariable String instanceId) {
        return approvalHistoryService.getHistoryInstanceDetail(instanceId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }
}
