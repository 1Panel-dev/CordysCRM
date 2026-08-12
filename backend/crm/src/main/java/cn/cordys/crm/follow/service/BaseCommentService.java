package cn.cordys.crm.follow.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithCommentCount;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.constants.FollowUpCommentTargetType;
import cn.cordys.crm.follow.domain.Comment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentPageRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.mapper.ExtCommentMapper;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源评论通用业务。子类仅提供资源类型、评论表 Mapper 和资源权限校验。
 */
@Transactional(rollbackFor = Exception.class)
public abstract class BaseCommentService<C extends Comment> {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private ExtCommentMapper extCommentMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    protected abstract BaseMapper<C> getCommentMapper();

    protected abstract C newComment();

    protected abstract FollowUpCommentTargetType getTargetType();

    protected abstract String getNotificationModule();

    protected abstract String getCommentAddedEvent();

    protected abstract String getCommentMentionedEvent();

    protected abstract void updateResourceCommentCount(String resourceId, String orgId, long commentCount);

    protected abstract CommentResourceInfo getResource(String resourceId, String orgId, String userId);

    public PagerWithCommentCount<List<CommentResponse>> page(CommentPageRequest request, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());

        List<CommentResponse> comments = extCommentMapper.selectPage(
                getTargetType().name(), request.getResourceId(), null, orgId);
        List<CommentResponse> children = comments.isEmpty() ? List.of() : extCommentMapper.selectChildren(
                getTargetType().name(), comments.stream().map(CommentResponse::getId).toList(), orgId);

        Map<String, List<CommentResponse>> childMap = children.stream()
                .collect(Collectors.groupingBy(CommentResponse::getParentId, LinkedHashMap::new, Collectors.toList()));
        comments.forEach(comment -> {
            List<CommentResponse> replies = childMap.getOrDefault(comment.getId(), List.of());
            comment.setReplies(replies);
            comment.setReplyCount(replies.size());
        });

        long commentCount = extCommentMapper.countByResource(getTargetType().name(), request.getResourceId(), orgId);
        return PageUtils.setPageInfoWithCommentCount(page, comments, commentCount);
    }

    public C add(CommentAddRequest request, String userId, String orgId) {
        long now = System.currentTimeMillis();
        C comment = newComment();
        comment.setId(IDGenerator.nextStr());
        comment.setResourceId(request.getResourceId());
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setContent(request.getContent());
        comment.setOrganizationId(orgId);
        comment.setCreateUser(userId);
        comment.setUpdateUser(userId);
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        getCommentMapper().insert(comment);
        updateCommentCount(request.getResourceId(), orgId);

        sendNotifications(request.getResourceId(), request.getReplyToUserId(), request.getMentionedUserIds(), userId, orgId);
        return comment;
    }

    private void sendNotifications(String resourceId, String replyToUserId, List<String> mentionedUserIds, String userId, String orgId) {
        List<String> noticeMentionedUserIds = mergeNotificationUsers(mentionedUserIds, replyToUserId);
        CommentResourceInfo resourceInfo = getResource(resourceId, orgId, userId);
        sendNotifications(resourceInfo, resourceId, noticeMentionedUserIds, userId, orgId);
    }

    public C update(CommentUpdateRequest request, String userId, String orgId) {
        C comment = getAndCheckOwnComment(request.getId(), userId, orgId);
        comment.setContent(request.getContent());
        comment.setUpdateUser(userId);
        comment.setUpdateTime(System.currentTimeMillis());
        getCommentMapper().update(comment);

        sendNotifications(comment.getResourceId(), comment.getReplyToUserId(), request.getMentionedUserIds(), userId, orgId);
        return comment;
    }

    public void delete(String id, String userId, String orgId) {
        C comment = getAndCheckOwnComment(id, userId, orgId);
        List<String> ids = new ArrayList<>();
        ids.add(id);

        LambdaQueryWrapper<C> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(Comment::getParentId, id);
        childQuery.eq(Comment::getOrganizationId, orgId);
        ids.addAll(getCommentMapper().selectListByLambda(childQuery).stream().map(Comment::getId).toList());

        getCommentMapper().deleteByIds(ids);
        updateCommentCount(comment.getResourceId(), orgId);
    }

    private void updateCommentCount(String resourceId, String orgId) {
        long commentCount = extCommentMapper.countByResource(getTargetType().name(), resourceId, orgId);
        updateResourceCommentCount(resourceId, orgId, commentCount);
    }

    protected CommentResourceInfo buildTargetInfo(String owner, String clueId, String customerId, String opportunityId) {
        if (StringUtils.isNotBlank(opportunityId)) {
            Opportunity opportunity = opportunityMapper.selectByPrimaryKey(opportunityId);
            if (opportunity != null) {
                return new CommentResourceInfo(owner, opportunity.getName());
            }
        }
        if (StringUtils.isNotBlank(clueId)) {
            Clue clue = clueMapper.selectByPrimaryKey(clueId);
            if (clue != null) {
                return new CommentResourceInfo(owner, clue.getName());
            }
        }
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        return new CommentResourceInfo(owner, customer == null ? "" : customer.getName());
    }

    private C getComment(String id, String orgId) {
        C comment = getCommentMapper().selectByPrimaryKey(id);
        if (comment == null || !Objects.equals(comment.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("follow.comment.not_found"));
        }
        return comment;
    }

    private C getAndCheckOwnComment(String id, String userId, String orgId) {
        C comment = getComment(id, orgId);
        if (!Strings.CS.equals(comment.getCreateUser(), userId)) {
            throw new GenericException(Translator.get("follow.comment.only_creator"));
        }
        return comment;
    }

    private List<String> mergeNotificationUsers(List<String> mentionedUserIds, String replyToUserId) {
        List<String> userIds = new ArrayList<>(mentionedUserIds == null ? List.of() : mentionedUserIds);
        if (StringUtils.isNotBlank(replyToUserId)) {
            userIds.add(replyToUserId);
        }
        return userIds;
    }

    private void sendNotifications(CommentResourceInfo resourceInfo, String resourceId, List<String> mentionedUserIds,
                                   String operator, String orgId) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", resourceInfo.name());
        resource.put("resourceId", resourceId);
        resource.put("targetType", getTargetType().name());
        String taskType = getNotificationModule();

        Set<String> mentioned = new LinkedHashSet<>(mentionedUserIds);
        if (StringUtils.isNotBlank(resourceInfo.owner()) && !mentioned.contains(resourceInfo.owner())) {
            commonNoticeSendService.sendNotice(taskType, getCommentAddedEvent(),
                    resource, operator, orgId, List.of(resourceInfo.owner()), true);
        }
        if (!mentioned.isEmpty()) {
            commonNoticeSendService.sendNotice(taskType, getCommentMentionedEvent(),
                    resource, operator, orgId, new ArrayList<>(mentioned), true);
        }
    }

    protected record CommentResourceInfo(String owner, String name) {
    }
}
