package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ApprovalCommentNoticeService {

    @Resource(name = "extApprovalCommentMapper")
    private ExtApprovalCommentMapper extApprovalCommentMapper;
    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    /**
     * 新增(notifyOwner=true)/编辑(notifyOwner=false)审批评论后按消息任务配置发送通知:
     * 1. APPROVAL_COMMENT -> 业务负责人;
     * 2. APPROVAL_COMMENT_REPALY -> 被回复人(仅新增时);
     * 3. APPROVAL_COMMENT_MENTIONED -> 被@的人(编辑时仅本次新增的@)。
     */
    public void sendNotice(ApprovalComment comment, List<String> userIds, boolean notifyOwner) {
        if (!notifyOwner && CollectionUtils.isEmpty(userIds)) {
            return;
        }
        try {
            ApprovalInstance instance = extApprovalCommentMapper.selectLatestInstance(comment.getResourceId(),
                    comment.getOrganizationId());
            if (instance == null) {
                return;
            }
            FormKey formKey = FormKey.ofKey(instance.getType());
            List<String> mentionedUserIds = CollectionUtils.isEmpty(userIds) ? List.of()
                    : userIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
            String owner = resolveOwner(comment, formKey, notifyOwner);
            // 负责人同时被@时只按@提醒, 避免重复通知
            if (StringUtils.isNotBlank(owner) && !mentionedUserIds.contains(owner)) {
                sendCommentNotice(NotificationConstants.Event.APPROVAL_COMMENT, comment, instance, formKey,
                        NoticeSendContext.builder().newValues(Map.of("owner", owner)));
            }
            if (notifyOwner && StringUtils.isNotBlank(comment.getReplyToUserId())) {
                sendCommentNotice(NotificationConstants.Event.APPROVAL_COMMENT_REPALY, comment, instance, formKey,
                        NoticeSendContext.builder().replyToUserId(comment.getReplyToUserId()));
            }
            if (CollectionUtils.isNotEmpty(mentionedUserIds)) {
                sendCommentNotice(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, comment, instance, formKey,
                        NoticeSendContext.builder().mentionedUserIds(mentionedUserIds));
            }
        } catch (Exception e) {
            // 通知在评论提交后发送，失败不能影响已保存的评论。
            log.warn("审批评论通知发送失败，commentId={}", comment.getId(), e);
        }
    }

    /**
     * 报价单没有业务负责人，不能以创建人替代。
     */
    private String resolveOwner(ApprovalComment comment, FormKey formKey, boolean notifyOwner) {
        if (!notifyOwner || formKey == FormKey.QUOTATION) {
            return null;
        }
        return extApprovalInstanceMapper.getResourceOwner(ApprovalResourceService.resolveApprovalTableName(formKey),
                comment.getResourceId(), comment.getOrganizationId());
    }

    private void sendCommentNotice(String event, ApprovalComment comment, ApprovalInstance instance, FormKey formKey,
            NoticeSendContext.NoticeSendContextBuilder builder) {
        Map<String, Object> paramMap = new HashMap<>(3);
        // 站内通知按业务模块分类与跳转，resourceId 保持评论维度。
        paramMap.put("resourceType", resolveResourceType(formKey));
        paramMap.put("resourceId", comment.getId());
        paramMap.put("name", StringUtils.defaultString(instance.getResourceName()));
        commonNoticeSendService.sendNoticeByConfig(builder
                .taskType(NotificationConstants.Module.APPROVAL)
                .action(event)
                .operatorId(comment.getUpdateUser())
                .organizationId(comment.getOrganizationId())
                .resourceId(comment.getResourceId())
                .paramMap(paramMap)
                .excludeSelf(true)
                .eventOnly(true)
                .build());
    }

    private String resolveResourceType(FormKey formKey) {
        return switch (formKey) {
            case CLUE -> NotificationConstants.Module.CLUE;
            case CUSTOMER -> NotificationConstants.Module.CUSTOMER;
            case OPPORTUNITY, QUOTATION -> NotificationConstants.Module.OPPORTUNITY;
            case CONTRACT, INVOICE -> NotificationConstants.Module.CONTRACT;
            case ORDER -> NotificationConstants.Module.ORDER;
            case null, default -> NotificationConstants.Module.APPROVAL;
        };
    }
}
