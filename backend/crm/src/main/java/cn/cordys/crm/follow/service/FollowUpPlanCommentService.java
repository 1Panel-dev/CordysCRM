package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.constants.ModuleKey;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE)
    public FollowUpPlanComment add(CommentAddRequest request, String userId, String orgId) {
        return super.add(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE)
    public FollowUpPlanComment update(CommentUpdateRequest request, String userId, String orgId) {
        return super.update(request, userId, orgId);
    }

    @Override
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE)
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
    protected String getCommentMentionTable() {
        return "follow_up_plan_comment_mention";
    }

    @Override
    protected String getCommentAddedEvent(String resourceId) {
        FollowUpPlan plan = planMapper.selectByPrimaryKey(resourceId);
        if (plan != null) {
            if (Strings.CI.equals(plan.getType(), ModuleKey.CLUE.name())) {
                return NotificationConstants.Event.CLUE_FOLLOW_UP_PLAN_COMMENT_ADDED;
            } else if (StringUtils.isNotBlank(plan.getOpportunityId())) {
                return NotificationConstants.Event.OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_ADDED;
            } else {
                return NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED;
            }
        }
        return NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED;
    }

    @Override
    protected String getCommentMentionedEvent(String resourceId) {
        FollowUpPlan plan = planMapper.selectByPrimaryKey(resourceId);
        if (plan != null) {
            if (Strings.CI.equals(plan.getType(), ModuleKey.CLUE.name())) {
                return NotificationConstants.Event.CLUE_FOLLOW_UP_PLAN_COMMENT_MENTIONED;
            } else if (StringUtils.isNotBlank(plan.getOpportunityId())) {
                return NotificationConstants.Event.OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_MENTIONED;
            } else {
                return NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED;
            }
        }
        return NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED;
    }

    @Override
    protected void updateResourceCommentCount(String resourceId, String orgId, long commentCount) {
        extPlanMapper.updateCommentCount(resourceId, orgId, commentCount);
    }

    @Override
    protected CommentResourceInfo getNoticeResource(String resourceId, String orgId) {
        FollowUpPlan plan = planMapper.selectByPrimaryKey(resourceId);
        if (plan == null || !Objects.equals(plan.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.target_not_found"));
        }
        return buildTargetInfo(plan.getType(), plan.getOwner(), plan.getClueId(), plan.getCustomerId(), plan.getOpportunityId());
    }
}
