package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithCommentCount;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.constants.FollowUpCommentTargetType;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.domain.FollowUpRecordComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentPageRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FollowUpRecordCommentService extends BaseCommentService<FollowUpRecordComment> {

    @Resource
    private BaseMapper<FollowUpRecordComment> commentMapper;
    @Resource
    private BaseMapper<FollowUpRecord> recordMapper;
    @Resource
    private ExtFollowUpRecordMapper extRecordMapper;

    @Override
    public PagerWithCommentCount<List<CommentResponse>> page(CommentPageRequest request, String orgId) {
        return super.page(request, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.UPDATE)
    public FollowUpRecordComment add(CommentAddRequest request, String userId, String orgId) {
        return super.add(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.UPDATE)
    public FollowUpRecordComment update(CommentUpdateRequest request, String userId, String orgId) {
        return super.update(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.UPDATE)
    public FollowUpRecordComment delete(String id, String userId, String orgId) {
        return super.delete(id, userId, orgId);
    }

    @Override
    protected BaseMapper<FollowUpRecordComment> getCommentMapper() {
        return commentMapper;
    }

    @Override
    protected FollowUpRecordComment newComment() {
        return new FollowUpRecordComment();
    }

    @Override
    protected FollowUpCommentTargetType getTargetType() {
        return FollowUpCommentTargetType.RECORD;
    }

    @Override
    protected String getCommentMentionTable() {
        return "follow_up_record_comment_mention";
    }

    @Override
    protected void updateResourceCommentCount(String resourceId, String orgId, long commentCount) {
        extRecordMapper.updateCommentCount(resourceId, orgId, commentCount);
    }

    @Override
    protected CommentResourceInfo getNoticeResource(String resourceId, String orgId) {
        FollowUpRecord record = recordMapper.selectByPrimaryKey(resourceId);
        if (record == null || !Objects.equals(record.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.target_not_found"));
        }
        return buildTargetInfo(record.getType(), record.getOwner(), record.getClueId(), record.getCustomerId(),
                record.getOpportunityId());
    }

    /**
     * 新增跟进记录评论后, 按新版消息任务配置发送通知
     */
    @Override
    protected void sendCommentAddedNotifications(CommentResourceInfo resourceInfo, String resourceId,
            String replyToUserId, List<String> mentionedUserIds, String operator, String orgId) {
        sendRecordCommentNotices(resourceInfo, resourceId, replyToUserId, mentionedUserIds, operator, orgId);
    }

    /**
     * 编辑跟进记录评论后, 按新版消息任务配置发送通知
     */
    @Override
    protected void sendCommentUpdatedNotifications(CommentResourceInfo resourceInfo, String resourceId,
            String replyToUserId, List<String> mentionedUserIds, String operator, String orgId) {
        sendRecordCommentNotices(resourceInfo, resourceId, replyToUserId, mentionedUserIds, operator, orgId);
    }

    /**
     * 1. 跟进记录被评论(FOLLOW_UP_RECORD_COMMENT) -> 记录负责人;
     * 2. 评论被回复(FOLLOW_UP_RECORD_COMMENT_REPALY) -> 被回复人;
     * 3. 评论@用户(FOLLOW_UP_RECORD_COMMENT_MENTIONED) -> 被@的人。
     */
    private void sendRecordCommentNotices(CommentResourceInfo resourceInfo, String resourceId, String replyToUserId,
            List<String> mentionedUserIds, String operator, String orgId) {
        if (StringUtils.isNotBlank(resourceInfo.owner())) {
            sendCommentNotice(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT, resourceId, resourceInfo,
                    operator, orgId, NoticeSendContext.builder().newValues(Map.of("owner", resourceInfo.owner())));
        }
        if (StringUtils.isNotBlank(replyToUserId)) {
            sendCommentNotice(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY, resourceId, resourceInfo,
                    operator, orgId, NoticeSendContext.builder().replyToUserId(replyToUserId));
        }
        if (CollectionUtils.isNotEmpty(mentionedUserIds)) {
            sendCommentNotice(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED, resourceId,
                    resourceInfo, operator, orgId, NoticeSendContext.builder().mentionedUserIds(mentionedUserIds));
        }
    }

    private void sendCommentNotice(String event, String resourceId, CommentResourceInfo resourceInfo,
            String operator, String orgId, NoticeSendContext.NoticeSendContextBuilder builder) {
        commonNoticeSendService.sendNoticeByConfig(builder
                .taskType(NotificationConstants.Module.FOLLOW_UP)
                .action(event)
                .operatorId(operator)
                .organizationId(orgId)
                .resourceId(resourceId)
                .resourceName(resourceInfo.name())
                .excludeSelf(true)
                .eventOnly(true)
                .build());
    }
}
