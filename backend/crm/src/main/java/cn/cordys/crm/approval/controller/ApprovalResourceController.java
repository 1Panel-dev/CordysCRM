package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                    approvalCommentService.hasCommentPermission(resourceId, SessionUtils.getUserId(), orgId));
        }
        return detail;
    }

    @GetMapping("/instance-detail/{resourceId}")
    @Operation(summary = "查询历史审批详情，按轮次由远至近返回")
    public List<ApprovalInstanceDetail> getHistoryInstanceDetail(@PathVariable String resourceId) {
        return approvalHistoryService.getHistoryInstanceDetail(resourceId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }
}
