package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalCommentMention;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.request.ApprovalCommentAddRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentUpdateRequest;
import cn.cordys.crm.approval.dto.response.ApprovalCommentMentionUserResponse;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalCommentService {

    @Resource
    private BaseMapper<ApprovalComment> commentMapper;
    @Resource
    private BaseMapper<ApprovalCommentMention> mentionMapper;
    @Resource(name = "extApprovalCommentMapper")
    private ExtApprovalCommentMapper extCommentMapper;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private ApprovalCommentNoticeService approvalCommentNoticeService;

    public List<ApprovalCommentResponse> list(String instanceId, String userId, String orgId) {
        checkPermission(instanceId, userId, orgId);
        List<ApprovalCommentResponse> comments = extCommentMapper.selectList(instanceId, orgId);
        if (comments.isEmpty()) {
            return comments;
        }
        List<ApprovalCommentResponse> children = extCommentMapper.selectChildren(instanceId,
                comments.stream().map(ApprovalCommentResponse::getId).toList(), orgId);
        List<ApprovalCommentResponse> allComments = new ArrayList<>(comments);
        allComments.addAll(children);
        enrichComments(allComments, userId, orgId);
        Map<String, List<ApprovalCommentResponse>> replies = children.stream()
                .collect(Collectors.groupingBy(ApprovalCommentResponse::getParentId));
        comments.forEach(comment -> {
            List<ApprovalCommentResponse> commentReplies = replies.getOrDefault(comment.getId(), List.of());
            comment.setReplies(commentReplies);
            comment.setReplyCount(commentReplies.size());
        });
        return comments;
    }

    public ApprovalCommentResponse add(ApprovalCommentAddRequest request, String userId, String orgId) {
        checkPermission(request.getInstanceId(), userId, orgId);
        String parentId = StringUtils.trimToNull(request.getParentId());
        String replyToUserId = checkReply(request.getInstanceId(), parentId, request.getReplyToUserId(), orgId);
        List<String> mentionedUserIds = checkMentionUsers(request.getMentionedUserIds(), orgId);

        long now = System.currentTimeMillis();
        ApprovalComment comment = new ApprovalComment();
        comment.setId(IDGenerator.nextStr());
        comment.setInstanceId(request.getInstanceId());
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(request.getContent());
        comment.setOrganizationId(orgId);
        comment.setCreateUser(userId);
        comment.setUpdateUser(userId);
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        commentMapper.insert(comment);
        saveMentions(comment.getId(), mentionedUserIds, now);
        sendMentionNoticeAfterCommit(comment, mentionedUserIds);
        return loadResponse(comment.getId(), userId, orgId);
    }

    public ApprovalCommentResponse update(ApprovalCommentUpdateRequest request, String userId, String orgId) {
        ApprovalComment comment = getOwnComment(request.getId(), userId, orgId);
        List<String> mentionedUserIds = checkMentionUsers(request.getMentionedUserIds(), orgId);
        comment.setContent(request.getContent());
        comment.setUpdateUser(userId);
        comment.setUpdateTime(System.currentTimeMillis());
        commentMapper.update(comment);
        ApprovalCommentMention criteria = new ApprovalCommentMention();
        criteria.setCommentId(comment.getId());
        List<ApprovalCommentMention> existingMentions = mentionMapper.select(criteria);
        List<String> removedIds = existingMentions.stream()
                .filter(mention -> !mentionedUserIds.contains(mention.getUserId()))
                .map(ApprovalCommentMention::getId).toList();
        if (!removedIds.isEmpty()) {
            mentionMapper.deleteByIds(removedIds);
        }
        Set<String> existingUserIds = existingMentions.stream()
                .map(ApprovalCommentMention::getUserId).collect(Collectors.toSet());
        List<String> addedUserIds = mentionedUserIds.stream().filter(id -> !existingUserIds.contains(id)).toList();
        saveMentions(comment.getId(), addedUserIds, comment.getUpdateTime());
        sendMentionNoticeAfterCommit(comment, addedUserIds);
        // 本次编辑可能移除操作者最后一条有效@，返回结果不再次判权。
        return loadResponse(comment.getId(), userId, orgId);
    }

    public void delete(String id, String userId, String orgId) {
        ApprovalComment comment = getOwnComment(id, userId, orgId);
        List<String> ids = new ArrayList<>();
        ids.add(comment.getId());
        if (comment.getParentId() == null) {
            LambdaQueryWrapper<ApprovalComment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalComment::getParentId, comment.getId())
                    .eq(ApprovalComment::getInstanceId, comment.getInstanceId())
                    .eq(ApprovalComment::getOrganizationId, orgId);
            commentMapper.selectListByLambda(wrapper).forEach(child -> ids.add(child.getId()));
        }
        extCommentMapper.deleteMentions(ids, orgId);
        extCommentMapper.deleteComments(ids, comment.getInstanceId(), orgId);
    }

    /**
     * 判断当前用户是否具有指定审批实例的评论权限。
     */
    public boolean hasCommentPermission(String instanceId, String userId, String orgId) {
        ApprovalInstance instance = extCommentMapper.selectInstance(instanceId, orgId);
        return instance != null && hasCommentPermission(instance, userId, orgId);
    }

    public ApprovalInstance getMentionInstance(String mentionId, String resourceId, String userId, String orgId) {
        ApprovalInstance instance = extCommentMapper.selectMentionInstance(mentionId, userId, orgId);
        if (instance == null || !Objects.equals(resourceId, instance.getResourceId())) {
            throw new GenericException(Translator.get("approval.comment.no_permission"));
        }
        return instance;
    }

    private boolean hasCommentPermission(ApprovalInstance instance, String userId, String orgId) {
        // 评论权限: 业务审批数据的查看权限或评论被@用户
        return approvalResourceService.hasInstanceViewPermission(instance, userId, orgId)
                || extCommentMapper.existsMention(instance.getId(), userId, orgId);
    }

    private void checkPermission(String instanceId, String userId, String orgId) {
        ApprovalInstance instance = extCommentMapper.selectInstance(instanceId, orgId);
        if (instance == null) {
            throw new GenericException(Translator.get("approval.comment.instance_not_found"));
        }
        if (!hasCommentPermission(instance, userId, orgId)) {
            throw new GenericException(Translator.get("approval.comment.no_permission"));
        }
    }

    private ApprovalComment getOwnComment(String id, String userId, String orgId) {
        ApprovalComment comment = extCommentMapper.selectCommentForUpdate(id, orgId);
        if (comment == null || !Strings.CS.equals(comment.getOrganizationId(), orgId)) {
            throw new GenericException(Translator.get("approval.comment.not_found"));
        }
        checkPermission(comment.getInstanceId(), userId, orgId);
        if (!Strings.CS.equals(comment.getCreateUser(), userId)) {
            throw new GenericException(Translator.get("approval.comment.only_creator"));
        }
        return comment;
    }

    private String checkReply(String instanceId, String parentId, String replyToUserId, String orgId) {
        if (parentId == null) {
            if (StringUtils.isNotBlank(replyToUserId)) {
                throw new GenericException(Translator.get("approval.comment.invalid_reply_user"));
            }
            return null;
        }
        // 回复和删除一级评论共用行锁，避免删除后留下新回复。
        ApprovalComment parent = extCommentMapper.selectCommentForUpdate(parentId, orgId);
        if (parent == null || !Objects.equals(parent.getOrganizationId(), orgId)
                || !Objects.equals(parent.getInstanceId(), instanceId) || parent.getParentId() != null) {
            throw new GenericException(Translator.get("approval.comment.invalid_parent"));
        }
        if (StringUtils.isBlank(replyToUserId)) {
            return parent.getCreateUser();
        }
        if (!extCommentMapper.existsReplyUser(instanceId, parentId, replyToUserId, orgId)) {
            throw new GenericException(Translator.get("approval.comment.invalid_reply_user"));
        }
        return replyToUserId;
    }

    private List<String> checkMentionUsers(List<String> userIds, String orgId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<String> distinctIds = userIds.stream().distinct().toList();
        if (distinctIds.stream().anyMatch(StringUtils::isBlank)
                || !extCommentMapper.selectEnabledUserIds(distinctIds, orgId).containsAll(distinctIds)) {
            throw new GenericException(Translator.get("approval.comment.invalid_mention_user"));
        }
        return distinctIds;
    }

    private void saveMentions(String commentId, List<String> userIds, long createTime) {
        if (userIds.isEmpty()) {
            return;
        }
        List<ApprovalCommentMention> mentions = userIds.stream().map(userId -> {
            ApprovalCommentMention mention = new ApprovalCommentMention();
            mention.setId(IDGenerator.nextStr());
            mention.setCommentId(commentId);
            mention.setUserId(userId);
            mention.setCreateTime(createTime);
            return mention;
        }).toList();
        mentionMapper.batchInsert(mentions);
    }

    private void sendMentionNoticeAfterCommit(ApprovalComment comment, List<String> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    approvalCommentNoticeService.sendMentionNotice(comment, userIds);
                }
            });
        } else {
            approvalCommentNoticeService.sendMentionNotice(comment, userIds);
        }
    }

    private ApprovalCommentResponse getResponse(String id, String orgId) {
        ApprovalCommentResponse comment = extCommentMapper.selectDetail(id, orgId);
        if (comment == null) {
            throw new GenericException(Translator.get("approval.comment.not_found"));
        }
        return comment;
    }

    private ApprovalCommentResponse loadResponse(String id, String userId, String orgId) {
        ApprovalCommentResponse comment = getResponse(id, orgId);
        enrichComments(List.of(comment), userId, orgId);
        return comment;
    }

    private void enrichComments(List<ApprovalCommentResponse> comments, String userId, String orgId) {
        if (comments.isEmpty()) {
            return;
        }
        Map<String, List<ApprovalCommentMentionUserResponse>> mentionUsers = extCommentMapper.selectMentionUsers(
                        comments.stream().map(ApprovalCommentResponse::getId).toList(), orgId).stream()
                .collect(Collectors.groupingBy(ApprovalCommentMentionUserResponse::getCommentId));
        comments.forEach(comment -> {
            comment.setMentionUsers(mentionUsers.getOrDefault(comment.getId(), List.of()));
            comment.setEditable(Objects.equals(comment.getCreateUser(), userId));
        });
    }
}
