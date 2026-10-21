package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
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
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    public void sendMentionNotice(ApprovalComment comment, List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        try {
            if (extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.APPROVAL,
                    NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, comment.getOrganizationId()) == null) {
                return;
            }
            ApprovalInstance instance = extApprovalCommentMapper.selectInstance(comment.getInstanceId(), comment.getOrganizationId());
            if (instance == null) {
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
            String summary = StringUtils.normalizeSpace(StringUtils.defaultString(comment.getContent()));
            if (summary.codePointCount(0, summary.length()) > 100) {
                summary = summary.substring(0, summary.offsetByCodePoints(0, 100)) + "…";
            }
            Map<String, Object> params = new HashMap<>();
            params.put("name", StringUtils.defaultString(instance.getResourceName()));
            params.put("commentSummary", summary);
            params.put("resourceType", resourceType);
            params.put("resourceId", comment.getId());
            commonNoticeSendService.sendNotice(NotificationConstants.Module.APPROVAL,
                    NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, params, comment.getUpdateUser(),
                    comment.getOrganizationId(), userIds, false);
        } catch (Exception e) {
            // 通知在评论提交后发送，失败不能影响已保存的评论。
            log.warn("审批评论@通知发送失败，commentId={}", comment.getId(), e);
        }
    }
}
