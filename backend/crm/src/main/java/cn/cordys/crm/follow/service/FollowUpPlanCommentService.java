package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithCommentCount;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.constants.FollowUpCommentTargetType;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpPlanComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentPageRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.List;

@Service
public class FollowUpPlanCommentService extends BaseCommentService<FollowUpPlanComment> {

    @Resource
    private BaseMapper<FollowUpPlanComment> commentMapper;
    @Resource
    private BaseMapper<FollowUpPlan> planMapper;
    @Resource
    private ExtFollowUpPlanMapper extPlanMapper;

    @Override
    public PagerWithCommentCount<List<CommentResponse>> page(CommentPageRequest request, String orgId) {
        return super.page(request, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.ADD,
            resourceId = "{#request.targetId}", resourceName = "{#request.content}")
    public FollowUpPlanComment add(CommentAddRequest request, String userId, String orgId) {
        return super.add(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE,
            resourceId = "{#request.id}", resourceName = "{#request.content}")
    public FollowUpPlanComment update(CommentUpdateRequest request, String userId, String orgId) {
        return super.update(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.DELETE,
            resourceId = "{#id}", resourceName = "{#id}")
    public void delete(String id, String userId, String orgId) {
        super.delete(id, userId, orgId);
    }

    @Override
    protected BaseMapper<FollowUpPlanComment> getCommentMapper() {
        return commentMapper;
    }

    @Override
    protected FollowUpPlanComment newComment() {
        return new FollowUpPlanComment();
    }

    @Override
    protected FollowUpCommentTargetType getTargetType() {
        return FollowUpCommentTargetType.PLAN;
    }

    @Override
    protected String getNotificationModule() {
        return NotificationConstants.Module.FOLLOW_UP_PLAN;
    }

    @Override
    protected String getCommentAddedEvent() {
        return NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_ADDED;
    }

    @Override
    protected String getCommentMentionedEvent() {
        return NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_MENTIONED;
    }

    @Override
    protected void updateResourceCommentCount(String resourceId, String orgId, long commentCount) {
        extPlanMapper.updateCommentCount(resourceId, orgId, commentCount);
    }

    @Override
    protected CommentResourceInfo getResource(String resourceId, String orgId, String userId) {
        FollowUpPlan plan = planMapper.selectByPrimaryKey(resourceId);
        if (plan == null || !Objects.equals(plan.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.target_not_found"));
        }
        return buildTargetInfo(plan.getOwner(), plan.getClueId(), plan.getCustomerId(), plan.getOpportunityId());
    }
}
