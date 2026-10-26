package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithCommentCount;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalCommentMention;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.request.ApprovalCommentAddRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentUpdateRequest;
import cn.cordys.crm.approval.dto.response.ApprovalCommentMentionUserResponse;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
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
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private FileCommonService fileCommonService;

    public PagerWithCommentCount<List<ApprovalCommentResponse>> page(ApprovalCommentPageRequest request, String userId,
            String orgId) {
        String resourceId = request.getResourceId();
        checkPermission(resourceId, userId, orgId);
        ApprovalComment criteria = new ApprovalComment();
        criteria.setResourceId(resourceId);
        criteria.setOrganizationId(orgId);
        long commentCount = commentMapper.countByExample(criteria);
        boolean asc = request.getSort() != null && "asc".equals(request.getSort().getType());
        try (Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize())) {
            List<ApprovalCommentResponse> comments = extCommentMapper.selectPage(resourceId, orgId, asc);
            if (comments.isEmpty()) {
                return PageUtils.setPageInfoWithCommentCount(page, comments, commentCount);
            }
            List<ApprovalCommentResponse> children = extCommentMapper.selectChildren(resourceId,
                    comments.stream().map(ApprovalCommentResponse::getId).toList(), orgId, asc);
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
            return PageUtils.setPageInfoWithCommentCount(page, comments, commentCount);
        }
    }

    public ApprovalCommentResponse add(ApprovalCommentAddRequest request, String userId, String orgId) {
        checkPermission(request.getResourceId(), userId, orgId);
        String parentId = StringUtils.trimToNull(request.getParentId());
        String replyToUserId = checkReply(request.getResourceId(), parentId, request.getReplyToUserId(), orgId);
        List<String> mentionedUserIds = checkMentionUsers(request.getMentionedUserIds(), orgId);

        long now = System.currentTimeMillis();
        ApprovalComment comment = new ApprovalComment();
        comment.setId(IDGenerator.nextStr());
        comment.setResourceId(request.getResourceId());
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(request.getContent());
        comment.setOrganizationId(orgId);
        comment.setCreateUser(userId);
        comment.setUpdateUser(userId);
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        saveAttachments(comment.getId(), request.getAttachmentIds(), userId, orgId);
        commentMapper.insert(comment);
        saveMentions(comment.getId(), mentionedUserIds, now);
        sendMentionNoticeAfterCommit(comment, mentionedUserIds);
        return loadResponse(comment.getId(), userId, orgId);
    }

    public ApprovalCommentResponse update(ApprovalCommentUpdateRequest request, String userId, String orgId) {
        ApprovalComment comment = getOwnComment(request.getId(), userId, orgId);
        List<String> mentionedUserIds = checkMentionUsers(request.getMentionedUserIds(), orgId);
        saveAttachments(comment.getId(), request.getAttachmentIds(), userId, orgId);
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
                    .eq(ApprovalComment::getResourceId, comment.getResourceId())
                    .eq(ApprovalComment::getOrganizationId, orgId);
            commentMapper.selectListByLambda(wrapper).forEach(child -> ids.add(child.getId()));
        }
        removeAttachments(attachmentMapper.selectListByLambda(new LambdaQueryWrapper<Attachment>()
                .in(Attachment::getResourceId, ids).eq(Attachment::getOrganizationId, orgId)));
        extCommentMapper.deleteMentions(ids, orgId);
        extCommentMapper.deleteComments(ids, comment.getResourceId(), orgId);
    }

    /**
     * 判断当前用户是否具有指定业务资源的评论权限。
     */
    public boolean hasCommentPermission(String resourceId, String userId, String orgId) {
        ApprovalInstance instance = extCommentMapper.selectLatestInstance(resourceId, orgId);
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
        return approvalResourceService.hasViewPermission(instance.getResourceId(), userId, orgId)
                || extCommentMapper.existsMention(instance.getResourceId(), userId, orgId);
    }

    private void checkPermission(String resourceId, String userId, String orgId) {
        ApprovalInstance instance = extCommentMapper.selectLatestInstance(resourceId, orgId);
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
        checkPermission(comment.getResourceId(), userId, orgId);
        if (!Strings.CS.equals(comment.getCreateUser(), userId)) {
            throw new GenericException(Translator.get("approval.comment.only_creator"));
        }
        return comment;
    }

    private String checkReply(String resourceId, String parentId, String replyToUserId, String orgId) {
        if (parentId == null) {
            if (StringUtils.isNotBlank(replyToUserId)) {
                throw new GenericException(Translator.get("approval.comment.invalid_reply_user"));
            }
            return null;
        }
        // 回复和删除一级评论共用行锁，避免删除后留下新回复。
        ApprovalComment parent = extCommentMapper.selectCommentForUpdate(parentId, orgId);
        if (parent == null || !Objects.equals(parent.getOrganizationId(), orgId)
                || !Objects.equals(parent.getResourceId(), resourceId) || parent.getParentId() != null) {
            throw new GenericException(Translator.get("approval.comment.invalid_parent"));
        }
        if (StringUtils.isBlank(replyToUserId)) {
            return parent.getCreateUser();
        }
        if (!extCommentMapper.existsReplyUser(resourceId, parentId, replyToUserId, orgId)) {
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
        Map<String, List<Attachment>> attachments = attachmentMapper.selectListByLambda(
                        new LambdaQueryWrapper<Attachment>()
                                .in(Attachment::getResourceId, comments.stream().map(ApprovalCommentResponse::getId).toList())
                                .eq(Attachment::getOrganizationId, orgId)).stream()
                .collect(Collectors.groupingBy(Attachment::getResourceId));
        comments.forEach(comment -> {
            comment.setMentionUsers(mentionUsers.getOrDefault(comment.getId(), List.of()));
            comment.setAttachments(attachments.getOrDefault(comment.getId(), List.of()).stream()
                    .map(attachment -> BeanUtils.copyBean(new ApprovalCommentResponse.AttachmentResponse(), attachment))
                    .toList());
            comment.setEditable(Objects.equals(comment.getCreateUser(), userId));
        });
    }

    private void saveAttachments(String commentId, List<String> attachmentIds, String userId, String orgId) {
        if (attachmentIds == null) {
            return;
        }
        List<String> ids = attachmentIds.stream().distinct().toList();
        if (ids.stream().anyMatch(id -> id == null || !id.matches("[A-Za-z0-9_-]{1,32}"))) {
            throw new GenericException(Translator.get("approval.comment.invalid_attachment"));
        }
        Set<String> existingIds = ids.isEmpty() ? Set.of() : attachmentMapper.selectByIds(ids).stream()
                .peek(attachment -> {
                    if (!Objects.equals(orgId, attachment.getOrganizationId())
                            || !Objects.equals(commentId, attachment.getResourceId())) {
                        throw new GenericException(Translator.get("approval.comment.invalid_attachment"));
                    }
                }).map(Attachment::getId).collect(Collectors.toSet());
        List<String> newIds = ids.stream().filter(id -> !existingIds.contains(id)).toList();
        newIds.forEach(id -> checkAttachmentFile(DefaultRepositoryDir.getTempFileDir(id)));
        // 使用评论ID隔离附件，普通业务表单保存不会清理评论附件。
        List<Attachment> currentAttachments = attachmentMapper.selectListByLambda(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getResourceId, commentId).eq(Attachment::getOrganizationId, orgId));
        List<String> preservedIds = new ArrayList<>(ids);
        currentAttachments.stream().map(Attachment::getId)
                .filter(id -> !preservedIds.contains(id)).forEach(preservedIds::add);
        // 新文件确认转存成功前保留旧文件，避免转存失败后丢失原附件。
        attachmentService.processTemp(new UploadTransferRequest(orgId, commentId, userId, preservedIds));
        // 文件服务会捕获转存异常，确认目标文件存在后才允许评论保存成功。
        newIds.forEach(id -> checkAttachmentFile(DefaultRepositoryDir.getTransferFileDir(orgId, commentId, id)));
        removeAttachments(currentAttachments.stream().filter(attachment -> !ids.contains(attachment.getId())).toList());
    }

    private void checkAttachmentFile(String folder) {
        List<File> files = fileCommonService.getFolderFiles(new FileRequest(folder, StorageType.LOCAL.name(), null));
        if (files == null || files.stream().noneMatch(File::isFile)) {
            throw new GenericException(Translator.get("approval.comment.invalid_attachment"));
        }
    }

    private void removeAttachments(List<Attachment> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        attachmentMapper.deleteByIds(attachments.stream().map(Attachment::getId).toList());
        Runnable cleanup = () -> attachments.forEach(attachment -> fileCommonService.deleteFolder(new FileRequest(
                DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(),
                        attachment.getId()), StorageType.LOCAL.name(), null), true));
        // 数据库提交后才删除物理文件，评论编辑失败时仍能保留原附件。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }
}
