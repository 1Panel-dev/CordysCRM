package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ApprovalCommentNoticeService {

    @Resource(name = "extApprovalCommentMapper")
    private ExtApprovalCommentMapper extApprovalCommentMapper;
    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    public void sendNotice(ApprovalComment comment, List<String> userIds, boolean notifyOwner) {
        if (!notifyOwner && CollectionUtils.isEmpty(userIds)) {
            return;
        }
        try {
            if (extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.APPROVAL,
                    NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, comment.getOrganizationId()) == null) {
                return;
            }
            ApprovalInstance instance = extApprovalCommentMapper.selectLatestInstance(comment.getResourceId(),
                    comment.getOrganizationId());
            if (instance == null) {
                return;
            }
            Set<String> receivers = new LinkedHashSet<>(CollectionUtils.isEmpty(userIds) ? List.of() : userIds);
            FormKey formKey = FormKey.ofKey(instance.getType());
            String owner = null;
            // 报价单没有业务负责人，不能以创建人替代。
            if (notifyOwner && formKey != FormKey.QUOTATION) {
                owner = extApprovalInstanceMapper.getResourceOwner(
                        ApprovalResourceService.resolveApprovalTableName(formKey), comment.getResourceId(),
                        comment.getOrganizationId());
            }
            if (receivers.isEmpty() && StringUtils.isBlank(owner)) {
                return;
            }
            String resourceType = switch (FormKey.ofKey(instance.getType())) {
                case CLUE -> NotificationConstants.Module.CLUE;
                case CUSTOMER -> NotificationConstants.Module.CUSTOMER;
                case OPPORTUNITY, QUOTATION -> NotificationConstants.Module.OPPORTUNITY;
                case CONTRACT, INVOICE -> NotificationConstants.Module.CONTRACT;
                case ORDER -> NotificationConstants.Module.ORDER;
                case null, default -> NotificationConstants.Module.APPROVAL;
            };
            Map<String, Object> params = new HashMap<>();
            params.put("name", StringUtils.defaultString(instance.getResourceName()));
            params.put("useTemplate", "true");
            params.put("templateKey", StringUtils.isBlank(comment.getParentId())
                    ? "message.approval_comment_mentioned_text" : "message.approval_comment_reply_mentioned_text");
            params.put("resourceType", resourceType);
            params.put("resourceId", comment.getId());
            if (!receivers.isEmpty()) {
                commonNoticeSendService.sendNotice(NotificationConstants.Module.APPROVAL,
                        NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, params, comment.getUpdateUser(),
                        comment.getOrganizationId(), List.copyOf(receivers), false);
            }
            if (StringUtils.isNotBlank(owner) && !receivers.contains(owner)) {
                Map<String, Object> ownerParams = new HashMap<>(params);
                ownerParams.put("templateKey", StringUtils.isBlank(comment.getParentId())
                        ? "message.approval_comment_added_text" : "message.approval_comment_reply_added_text");
                commonNoticeSendService.sendNotice(NotificationConstants.Module.APPROVAL,
                        NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, ownerParams, comment.getUpdateUser(),
                        comment.getOrganizationId(), List.of(owner), false);
            }
        } catch (Exception e) {
            // 通知在评论提交后发送，失败不能影响已保存的评论。
            log.warn("审批评论通知发送失败，commentId={}", comment.getId(), e);
        }
    }
}
