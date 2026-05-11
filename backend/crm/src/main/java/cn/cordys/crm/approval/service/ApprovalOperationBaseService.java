package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalOperationBaseRequest;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import cn.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class ApprovalOperationBaseService extends InSiteNoticeSender {

    @Resource
    private BaseMapper<ApprovalFlowVersion> approvalFlowVersionMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
    @Resource
    private FileCommonService fileCommonService;
    @Resource
    private BaseMapper<Notification> notificationBaseMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MessagePublisher messagePublisher;


    /**
     * 保存加签附件
     *
     * @param attachmentIds
     * @param instanceId
     * @param userId
     * @param orgId
     * @param approvalElementId
     */
    public void addAttachment(List<String> attachmentIds, String instanceId, String userId, String orgId, String approvalElementId) {
        UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, approvalElementId, userId, attachmentIds);
        List<ApprovalInstanceAttachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<ApprovalInstanceAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalInstanceAttachment::getApprovalElementId, transferRequest.getResourceId());
        List<ApprovalInstanceAttachment> transferredAttachments = approvalInstanceAttachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredIds = transferredAttachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).toList();

        attachmentIds.stream().filter(tempFileId -> !transferredIds.contains(tempFileId)).forEach(tempFileId -> {
            FileRequest fileRequest = new FileRequest(DefaultRepositoryDir.getTmpDir() + "/" + tempFileId, StorageType.LOCAL.name(), null);
            List<File> folderTempFiles = fileCommonService.getFolderFiles(fileRequest);
            if (!CollectionUtils.isEmpty(folderTempFiles)) {
                File tempFile = folderTempFiles.getFirst();
                FileCopyRequest copyRequest = new FileCopyRequest(DefaultRepositoryDir.getTempFileDir(tempFileId),
                        DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), tempFileId),
                        tempFile.getName());
                fileCommonService.copyFile(copyRequest, StorageType.LOCAL.name());
                ApprovalInstanceAttachment attachment = new ApprovalInstanceAttachment();
                attachment.setId(IDGenerator.nextStr());
                attachment.setInstanceId(instanceId);
                attachment.setApprovalElementId(approvalElementId);
                attachment.setAttachmentId(tempFileId);
                attachments.add(attachment);
            }
        });
        approvalInstanceAttachmentMapper.batchInsert(attachments);

        List<String> removedIds = transferredIds.stream().filter(aId -> !transferRequest.getTempFileIds().contains(aId)).toList();
        if (!CollectionUtils.isEmpty(removedIds)) {
            LambdaQueryWrapper<ApprovalInstanceAttachment> duplicateQueryWrapper = new LambdaQueryWrapper<>();
            duplicateQueryWrapper.in(ApprovalInstanceAttachment::getAttachmentId, removedIds);
            approvalInstanceAttachmentMapper.deleteByLambda(duplicateQueryWrapper);
            removedIds.forEach(removeId -> {
                FileRequest fileRequest = new FileRequest(DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), removeId), StorageType.LOCAL.name(), null);
                fileCommonService.deleteFolder(fileRequest, true);
            });
        }

    }


    /**
     * 获取流程配置相关权限
     *
     * @param id 审批实例ID
     * @return 审批流版本
     */
    public ApprovalFlowVersion getFlowPermission(String id) {
        ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(id);
        if (approvalInstance == null || StringUtils.isBlank(approvalInstance.getFlowVersionId())) {
            return null;
        }
        return approvalFlowVersionMapper.selectByPrimaryKey(approvalInstance.getFlowVersionId());
    }


    /**
     * 按ACTION 新增task任務
     *
     * @param request
     * @param userId
     * @return
     */
    public ApprovalTask addApprovalTask(ApprovalOperationBaseRequest request, String userId, String action) {
        ApprovalTask approvalTask = new ApprovalTask();
        BeanUtils.copyBean(approvalTask, request);
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(userId);
        approvalTask.setUpdateUser(userId);
        approvalTask.setStatus(ApprovalStatus.APPROVING.name());
        approvalTask.setType(action);
        approvalTaskMapper.insert(approvalTask);
        return approvalTask;
    }


    public void sendNotice(ApprovalInstance instance, String resourceName, String orgId, String context) {
        Notification notification = new Notification();
        String id = IDGenerator.nextStr();
        notification.setId(id);
        notification.setSubject(Translator.get("notice.default.subject"));
        notification.setOrganizationId(orgId);
        notification.setOperator(InternalUser.ADMIN.name());
        notification.setOperation("APPROVAL_NOTICE");
        notification.setResourceId(instance.getResourceId());
        notification.setResourceType(instance.getType());
        notification.setResourceName(resourceName);
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setCreateTime(System.currentTimeMillis());
        notification.setReceiver(instance.getSubmitterId());
        notification.setContent(context.getBytes());
        notification.setCreateUser(InternalUser.ADMIN.name());
        notification.setUpdateUser(InternalUser.ADMIN.name());
        notification.setCreateTime(System.currentTimeMillis());
        notification.setUpdateTime(System.currentTimeMillis());
        notificationBaseMapper.insert(notification);
        String messageText = JSON.toJSONString(notification);
        //储存信息
        stringRedisTemplate.opsForZSet().add(USER_PREFIX + instance.getSubmitterId(), id, System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(MSG_PREFIX + id, messageText);
        // 限制 Redis 只存 5 条消息
        Set<String> oldNotificationIds = stringRedisTemplate.opsForZSet()
                .reverseRange(USER_PREFIX + instance.getSubmitterId(), 4, -1);
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(oldNotificationIds)) {
            for (String oldNotificationId : oldNotificationIds) {
                stringRedisTemplate.delete(MSG_PREFIX + oldNotificationId);
            }
        }
        stringRedisTemplate.opsForZSet().removeRange(USER_PREFIX + instance.getSubmitterId(), 0, -6);
        //更新用户的已读全部消息状态 0 为未读，1为已读
        stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + instance.getSubmitterId(), "False");

        // 发送消息
        NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
        noticeRedisMessage.setMessage(instance.getSubmitterId());
        noticeRedisMessage.setNoticeType(NotificationConstants.Type.SYSTEM_NOTICE.toString());
        messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
    }

}
