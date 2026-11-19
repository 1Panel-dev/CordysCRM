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
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
    public FollowUpPlanComment delete(String id, String userId, String orgId) {
        return super.delete(id, userId, orgId);
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
    protected void updateResourceCommentCount(String resourceId, String orgId, long commentCount) {
        extPlanMapper.updateCommentCount(resourceId, orgId, commentCount);
    }

    @Override
    protected CommentResourceInfo getNoticeResource(String resourceId, String orgId) {
        FollowUpPlan plan = planMapper.selectByPrimaryKey(resourceId);
        if (plan == null || !Objects.equals(plan.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.target_not_found"));
        }
        return buildTargetInfo(plan.getType(), plan.getOwner(), plan.getClueId(), plan.getCustomerId(),
                plan.getOpportunityId());
    }
}
