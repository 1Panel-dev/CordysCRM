package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
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
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.List;

@Service
public class FollowUpRecordCommentService extends BaseCommentService<FollowUpRecordComment> {

    @Resource
    private BaseMapper<FollowUpRecordComment> commentMapper;
    @Resource
    private BaseMapper<FollowUpRecord> recordMapper;
    @Resource
    private ExtFollowUpRecordMapper extRecordMapper;

    @Override
    public Pager<List<CommentResponse>> page(CommentPageRequest request, String orgId) {
        return super.page(request, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.ADD,
            resourceId = "{#request.targetId}", resourceName = "{#request.content}")
    public FollowUpRecordComment add(CommentAddRequest request, String userId, String orgId) {
        return super.add(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.UPDATE,
            resourceId = "{#request.id}", resourceName = "{#request.content}")
    public FollowUpRecordComment update(CommentUpdateRequest request, String userId, String orgId) {
        return super.update(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.DELETE,
            resourceId = "{#id}", resourceName = "{#id}")
    public void delete(String id, String userId, String orgId) {
        super.delete(id, userId, orgId);
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
    protected String getNotificationModule() {
        return NotificationConstants.Module.FOLLOW_UP_RECORD;
    }

    @Override
    protected String getCommentAddedEvent() {
        return NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_ADDED;
    }

    @Override
    protected String getCommentMentionedEvent() {
        return NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED;
    }

    @Override
    protected void updateResourceCommentCount(String resourceId, String orgId, long commentCount) {
        extRecordMapper.updateCommentCount(resourceId, orgId, commentCount);
    }

    @Override
    protected CommentResourceInfo getResource(String resourceId, String orgId, String userId) {
        FollowUpRecord record = recordMapper.selectByPrimaryKey(resourceId);
        if (record == null || !Objects.equals(record.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.target_not_found"));
        }
        return buildTargetInfo(record.getOwner(), record.getClueId(), record.getCustomerId(), record.getOpportunityId());
    }
}
