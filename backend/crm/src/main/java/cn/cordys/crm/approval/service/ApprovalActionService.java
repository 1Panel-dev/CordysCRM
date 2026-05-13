package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalActionService {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private BaseMapper<ApprovalFlowVersion> approvalFlowVersionMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalAddSignTask> approvalAddSignTasMapper;
    @Resource
    private ModuleFormService formService;
    @Resource
    private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;
    @Resource
    private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
    @Resource
    private AttachmentService attachmentService;
    @Resource
    private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private LogService logService;
    @Resource
    private SqlSessionFactory sqlSessionFactory;

    /**
     * 加签
     *
     * @param request 加签参数
     * @param userId  当前用户
     * @param orgId   当前组织
     */
    public void addSign(ApprovalAddSignRequest request, String userId, String orgId) {
        // 审批流是否允许加签
        ApprovalFlowVersion flowVersion = getFlowVersionOfInstanceId(request.getInstanceId());
        if (flowVersion == null || !flowVersion.getAllowAddSign()) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
        // 追加加签操作的待办任务
        ApprovalTask appendActionTask = appendSignTask(request, userId);
        ApprovalAddSignTask addSignTask = saveAddSignTask(request, appendActionTask.getId(), instance.getSubmitterId(), orgId);
        // 刷新被加签任务状态 && 插入审批记录
        processOldSignedTask(request, userId, orgId);
        // 保存加签附件
        saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), addSignTask.getId(), userId, orgId);

        //日志
        String resourceName = approvalResourceService.selectBusinessName(FormKey.valueOf(instance.getType()), instance.getResourceId());
        LogDTO logDTO = new LogDTO(orgId, instance.getResourceId(), userId, LogType.APPROVAL, request.getModule(), resourceName);
        logDTO.setModifiedValue(Translator.get("sign_approval"));
        logService.add(logDTO);
    }

    /**
     * 回退
     *
     * @param request 回退参数
     * @param userId  当前用户
     * @param orgId   当前组织
     */
    public void back(ApprovalReturnBackRequest request, String userId, String orgId) {
        // 追加退回操作的待办任务 && 保存退回记录 && 更新被退回任务状态
        appendBackTasks(request, userId, orgId);
        ApprovalReturnBackRecord backRecord = saveBackRecord(request, userId);
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        approvalTask.setAction(ApprovalAction.BACK.name());
        approvalTaskMapper.update(approvalTask);
        // 保存退回附件
        saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), backRecord.getId(), userId, orgId);

        //日志
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
        String resourceName = approvalResourceService.selectBusinessName(FormKey.valueOf(instance.getType()), instance.getResourceId());
        LogDTO logDTO = new LogDTO(orgId, instance.getResourceId(), userId, LogType.APPROVAL, request.getModule(), resourceName);
        logDTO.setModifiedValue(Translator.get("back_approval"));
        logService.add(logDTO);
    }

    /**
     * 撤回
     *
     * @param request       撤回参数
     * @param currentUserId 当前用户
     * @param orgId         当前组织
     */
    public void revoke(ApprovalRevokeRequest request, String currentUserId, String orgId) {

    }

    /**
     * 同意
     *
     * @param request       撤回参数
     * @param currentUserId 当前用户
     * @param currentOrgId  当前组织
     */
    public void approve(ApprovalActionRequest request, String currentUserId, String currentOrgId) {
        ApprovalTask currentTask = getTaskById(request.getId());
        // 同意: 追加下一个节点任务, 否则完成审批流程
        List<ApprovalTask> appendTasks = appendTask(currentTask, currentUserId, currentOrgId);
        currentTask.setAction(ApprovalAction.APPROVE.name());
        currentTask.setStatus(ApprovalStatus.APPROVED.name());
        currentTask.setUpdateUser(currentUserId);
        currentTask.setUpdateTime(System.currentTimeMillis());
        approvalTaskMapper.updateById(currentTask);

        saveApprovalRecord(currentTask, request.getComment(), request.getAttachmentIds(), currentUserId, currentOrgId);

        // TODO: 多人审批任务需判断当前任务节点最终执行状态是否通过
        if (org.apache.commons.collections.CollectionUtils.isEmpty(appendTasks)) {
            ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
            finishApprovalInstance(instance, ApprovalStatus.APPROVED.name(), currentUserId);
        }

        // 批量插入待办任务
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(appendTasks)) {
            approvalTaskMapper.batchInsert(appendTasks);
        }

        //日志
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
        String resourceName = approvalResourceService.selectBusinessName(FormKey.valueOf(instance.getType()), instance.getResourceId());
        LogDTO logDTO = new LogDTO(currentOrgId, instance.getResourceId(), currentUserId, LogType.APPROVAL, request.getModule(), resourceName);
        logDTO.setModifiedValue(Translator.get("agree_approval"));
        logService.add(logDTO);
    }

    /**
     * 驳回
     *
     * @param request       驳回参数
     * @param currentUserId 当前用户
     * @param currentOrgId  当前组织
     */
    public void reject(ApprovalActionRequest request, String currentUserId, String currentOrgId) {
        ApprovalTask currentTask = getTaskById(request.getId());
        /*
         * 驳回: 当前任务所属节点最终执行状态为驳回, 中断审批流程
         * 	TODO: 特殊场景: 节点多人审批, 会签及依次审批时直接整体走驳回逻辑; 反过来如果是或签, 则需要节点下审批任务都为驳回状态才整体走驳回逻辑
         */
        currentTask.setAction(ApprovalAction.REJECT.name());
        currentTask.setStatus(ApprovalStatus.UNAPPROVED.name());
        currentTask.setUpdateUser(currentUserId);
        currentTask.setUpdateTime(System.currentTimeMillis());
        approvalTaskMapper.updateById(currentTask);

        saveApprovalRecord(currentTask, request.getComment(), request.getAttachmentIds(), currentUserId, currentOrgId);

        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
        finishApprovalInstance(instance, ApprovalStatus.REVOKED.name(), currentUserId);

        //日志
        String resourceName = approvalResourceService.selectBusinessName(FormKey.valueOf(instance.getType()), instance.getResourceId());
        LogDTO logDTO = new LogDTO(currentOrgId, instance.getResourceId(), currentUserId, LogType.APPROVAL, request.getModule(), resourceName);
        logDTO.setModifiedValue(Translator.get("reject_approval"));
        logService.add(logDTO);
    }

    public void sendNotice(ApprovalInstance instance, String resourceName, String orgId, String context) {
        // Notification notification = new Notification();
        // String id = IDGenerator.nextStr();
        // notification.setId(id);
        // notification.setSubject(Translator.get("notice.default.subject"));
        // notification.setOrganizationId(orgId);
        // notification.setOperator(InternalUser.ADMIN.name());
        // notification.setOperation("APPROVAL_NOTICE");
        // notification.setResourceId(instance.getResourceId());
        // notification.setResourceType(instance.getType());
        // notification.setResourceName(resourceName);
        // notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        // notification.setStatus(NotificationConstants.Status.UNREAD.name());
        // notification.setCreateTime(System.currentTimeMillis());
        // notification.setReceiver(instance.getSubmitterId());
        // notification.setContent(context.getBytes());
        // notification.setCreateUser(InternalUser.ADMIN.name());
        // notification.setUpdateUser(InternalUser.ADMIN.name());
        // notification.setCreateTime(System.currentTimeMillis());
        // notification.setUpdateTime(System.currentTimeMillis());
        // notificationBaseMapper.insert(notification);
        // String messageText = JSON.toJSONString(notification);
        // //储存信息
        // stringRedisTemplate.opsForZSet().add(USER_PREFIX + instance.getSubmitterId(), id, System.currentTimeMillis());
        // stringRedisTemplate.opsForValue().set(MSG_PREFIX + id, messageText);
        // // 限制 Redis 只存 5 条消息
        // Set<String> oldNotificationIds = stringRedisTemplate.opsForZSet()
        //         .reverseRange(USER_PREFIX + instance.getSubmitterId(), 4, -1);
        // if (org.apache.commons.collections.CollectionUtils.isNotEmpty(oldNotificationIds)) {
        //     for (String oldNotificationId : oldNotificationIds) {
        //         stringRedisTemplate.delete(MSG_PREFIX + oldNotificationId);
        //     }
        // }
        // stringRedisTemplate.opsForZSet().removeRange(USER_PREFIX + instance.getSubmitterId(), 0, -6);
        // //更新用户的已读全部消息状态 0 为未读，1为已读
        // stringRedisTemplate.opsForValue().set(USER_READ_PREFIX + instance.getSubmitterId(), "False");
        //
        // // 发送消息
        // NoticeRedisMessage noticeRedisMessage = new NoticeRedisMessage();
        // noticeRedisMessage.setMessage(instance.getSubmitterId());
        // noticeRedisMessage.setNoticeType(NotificationConstants.Type.SYSTEM_NOTICE.toString());
        // messagePublisher.publish(TopicConstants.SSE_TOPIC, JSON.toJSONString(noticeRedisMessage));
    }

    /**
     * 获取流程配置相关权限
     *
     * @param id 审批实例ID
     * @return 审批流
     */
    private ApprovalFlowVersion getFlowVersionOfInstanceId(String id) {
        ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(id);
        return approvalFlowVersionMapper.selectByPrimaryKey(approvalInstance.getFlowVersionId());
    }

    /**
     * 保存加签任务的信息
     *
     * @param request 加签参数
     */
    private ApprovalAddSignTask saveAddSignTask(ApprovalAddSignRequest request, String taskId, String submitter, String currentOrgId) {
        ApprovalAddSignTask approvalAddSignTask = new ApprovalAddSignTask();
        approvalAddSignTask.setId(IDGenerator.nextStr());
        approvalAddSignTask.setTaskId(taskId);
        approvalAddSignTask.setSignNodeId(request.getNodeId());
        approvalAddSignTask.setType(request.getType());
        // 特殊场景: 如果是多人审批, 加签节点位置暂时都放在之前
        boolean multiApprover = approvalFlowService.isCurrentNodeMultiApprover(request.getNodeId(), submitter, currentOrgId);
        if (multiApprover) {
            approvalAddSignTask.setType(ApprovalAddSignType.BEFORE.name());
        }
        approvalAddSignTask.setComment(request.getComment());
        approvalAddSignTasMapper.insert(approvalAddSignTask);
        return approvalAddSignTask;
    }

    /**
     * 刷新被加签任务信息
     *
     * @param request 加签参数
     */
    private void processOldSignedTask(ApprovalAddSignRequest request, String currentUserId, String currentOrgId) {
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.BEFORE.name())) {
            // 之前：加签
            approvalTask.setAction(ApprovalAction.SIGN.name());
        }
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.AFTER.name())) {
            // 之后：同意
            approvalTask.setAction(ApprovalAction.APPROVE.name());
            approvalTask.setStatus(ApprovalStatus.APPROVED.name());
        }
        approvalTaskMapper.update(approvalTask);
        saveApprovalRecord(approvalTask, request.getComment(), request.getAttachmentIds(), currentUserId, currentOrgId);
    }

    /**
     * 保存执行附件信息
     *
     * @param attachmentIds 附件ID集合
     * @param instanceId    实例ID
     * @param elementId     节点ID
     * @param userId        当前用户
     * @param orgId         当前组织
     */
    private void saveInstanceAttachment(List<String> attachmentIds, String instanceId, String elementId, String userId, String orgId) {
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(instanceId);
        List<ApprovalInstanceAttachment> attachments = new ArrayList<>();
        attachmentIds.forEach(attachmentId -> {
            ApprovalInstanceAttachment attachment = new ApprovalInstanceAttachment();
            attachment.setId(IDGenerator.nextStr());
            attachment.setInstanceId(instanceId);
            attachment.setElementId(elementId);
            attachment.setAttachmentId(attachmentId);
            attachments.add(attachment);
        });
        approvalInstanceAttachmentMapper.batchInsert(attachments);
        // 转移临时文件, 保存附件信息
        UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, instance.getResourceId(), userId, attachmentIds);
        attachmentService.appendTemp(transferRequest);
    }

    /**
     * 追加加签操作的待办任务
     *
     * @param request 加签参数
     * @param userId  当前用户
     */
    private ApprovalTask appendSignTask(ApprovalActionRequest request, String userId) {
        ApprovalTask approvalTask = new ApprovalTask();
        BeanUtils.copyBean(approvalTask, request);
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(userId);
        approvalTask.setUpdateUser(userId);
        approvalTask.setStatus(ApprovalStatus.APPROVING.name());
        approvalTask.setType(ApprovalAction.SIGN.name());
        approvalTaskMapper.insert(approvalTask);
        return approvalTask;
    }

    /**
     * 追加退回操作的待办任务
     *
     * @param backRequest  退回参数
     * @param userId       当前用户
     * @param currentOrgId 当前组织ID
     */
    private void appendBackTasks(ApprovalReturnBackRequest backRequest, String userId, String currentOrgId) {
        saveNodeApproverTasks(backRequest.getReturnToNodeId(), backRequest.getInstanceId(), userId, currentOrgId, ApprovalTaskType.BK.name());
    }

    /**
     * 保存退回节点信息
     *
     * @param request 退回参数
     * @param userId  当前用户ID
     * @return 退回节点信息
     */
    private ApprovalReturnBackRecord saveBackRecord(ApprovalReturnBackRequest request, String userId) {
        ApprovalReturnBackRecord returnBack = new ApprovalReturnBackRecord();
        returnBack.setId(IDGenerator.nextStr());
        returnBack.setTaskId(request.getId());
        returnBack.setReturnToNodeId(request.getReturnToNodeId());
        returnBack.setReturnReason(request.getComment());
        returnBack.setReturnUserId(userId);
        approvalReturnBackRecordMapper.insert(returnBack);
        return returnBack;
    }

    /**
     * 保存审批节点待办任务
     *
     * @param currentNodeId 当前任务ID
     * @param instanceId    审批实例ID
     * @param userId        当前用户ID
     * @param currentOrgId  当前组织ID
     * @param taskType      任务类型
     */
    public void saveNodeApproverTasks(String currentNodeId, String instanceId, String userId, String currentOrgId, String taskType) {
        List<ApprovalTask> approvalTasks = new ArrayList<>();
        ApprovalNodeApprover approvalNodeApprover = approvalNodeApproverMapper.selectByPrimaryKey(currentNodeId);
        List<User> approvers = approvalFlowService.getCurrentNodeMultiApprover(currentNodeId, userId, currentOrgId);
        if (Strings.CI.equals(approvalNodeApprover.getMultiApproverMode(), MultiApproverModeEnum.SEQUENTIAL.name()) || approvers.size() == 1) {
            // 单人或者依次审批, 只会产生一条待办任务
            User approverUser = approvers.getFirst();
            approvalTasks.add(buildTask(currentNodeId, instanceId, approverUser.getId(), taskType, userId));
        } else {
            // 多人审批, 且为会签或签方式
            approvers.forEach(approver -> {
                ApprovalTask approvalTask = buildTask(currentNodeId, instanceId, approver.getId(), taskType, userId);
                approvalTasks.add(approvalTask);
            });
        }
        if (CollectionUtils.isNotEmpty(approvalTasks)) {
            approvalTaskMapper.batchInsert(approvalTasks);
        }
    }

    private ApprovalTask buildTask(String nodeId, String instanceId, String approverId, String taskType, String currentUserId) {
        ApprovalTask approvalTask = new ApprovalTask();
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setNodeId(nodeId);
        approvalTask.setInstanceId(instanceId);
        approvalTask.setApproverId(approverId);
        approvalTask.setStatus(ApprovalStatus.APPROVING.name());
        approvalTask.setType(StringUtils.isBlank(taskType) ? ApprovalTaskType.NL.name() : taskType);
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(currentUserId);
        approvalTask.setUpdateUser(currentUserId);
        return approvalTask;
    }

    /**
     * 保存审批记录信息 (附件)
     *
     * @param currentTask   当前执行任务
     * @param comment       评论意见
     * @param attachmentIds 附件ID集合
     * @param currentUserId 当前操作人
     */
    private void saveApprovalRecord(ApprovalTask currentTask, String comment, List<String> attachmentIds, String currentUserId, String orgId) {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(IDGenerator.nextStr());
        record.setInstanceId(currentTask.getInstanceId());
        record.setTaskId(currentTask.getId());
        record.setNodeId(currentTask.getNodeId());
        record.setComment(comment);
        record.setCreateTime(System.currentTimeMillis());
        record.setCreateUser(currentUserId);
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(currentUserId);
        approvalRecordMapper.insert(record);
        saveInstanceAttachment(attachmentIds, currentTask.getInstanceId(), record.getId(), currentUserId, orgId);
    }

    /**
     * 根据任务ID获取审批任务
     *
     * @param taskId 任务ID
     * @return 审批任务
     */
    private ApprovalTask getTaskById(String taskId) {
        ApprovalTask currentTask = approvalTaskMapper.selectByPrimaryKey(taskId);
        if (currentTask == null) {
            throw new GenericException("审批任务不存在!");
        }
        return currentTask;
    }

    /**
     * 刷新最终实例状态
     *
     * @param instance       审批实例
     * @param approvalStatus 审批状态
     * @param currentUserId  当前用户ID
     */
    private void finishApprovalInstance(ApprovalInstance instance, String approvalStatus, String currentUserId) {
        instance.setApprovalStatus(approvalStatus);
        instance.setApprovalTime(System.currentTimeMillis());
        instance.setUpdateUser(currentUserId);
        instance.setUpdateTime(System.currentTimeMillis());
        approvalInstanceMapper.updateById(instance);
    }

    /**
     * 根据不同执行操作, 追加待办任务
     *
     * @param currentTask 当前任务
     */
    private List<ApprovalTask> appendTask(ApprovalTask currentTask, String currentUserId, String orgId) {
        List<ApprovalTask> approvalTasks = new ArrayList<>();
        // 多种情况需要新生成待办任务: 1. 正常流转到下一个节点, 包括退回&加签类型 2. 同一节点内配置的多人依次审批, 直接获取下一个审批人
        if (Strings.CI.equals(currentTask.getType(), ApprovalTaskType.SN.name())) {
            /*
             * TODO: 加签类型任务 (如果是目标任务之前的加签, 则取目标任务重新生成待办; 如果是之后的加签节点, 则取目标任务的下一个节点 *注意多人审批方式中的会签或签除外 )
             */
            // ApprovalAddSignTask signCriteria = new ApprovalAddSignTask();
            // signCriteria.setTaskId(currentTask.getId());
            // ApprovalAddSignTask signTask = approvalAddSignTaskMapper.selectOne(signCriteria);
            // ApprovalTask oldSignTask = getTaskById(signTask.getSignTaskId());
            // ApprovalNodeApprover nodeApprover = getNodeApprover(oldSignTask.getNodeId());
            // boolean multiApprover = isMultiApprover(nodeApprover);
            // if (!multiApprover ||  Strings.CI.equals(nodeApprover.getMultiApproverMode(), MultiApproverModeEnum.SEQUENTIAL.name())) {
            // 	if (Strings.CI.equals(signTask.getType(), ApprovalAddSignType.BEFORE.name())) {
            // 		oldSignTask.setId(IDGenerator.nextStr());
            // 		oldSignTask.setStatus(ApprovalStatus.APPROVING.name());
            // 		oldSignTask.setAction(null);
            // 		approvalTaskMapper.insert(oldSignTask);
            // 	} else {
            // 		if (multiApprover) {
            // 			// 多人且依次审批, 则生成待办任务
            // 		} else {
            // 			// 单人审批, 取下一个节点生成待办任务
            // 		}
            // 	}
            // }
        } else {
            ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
            List<BaseModuleFieldValue> fvs = formService.compressResourceDetail(instance.getType(), instance.getResourceId());
            ApprovalNodeResponse nextNode = approvalFlowService.getNextNode(currentTask.getNodeId(), fvs);
            //TODO: 处理下一个节点(approvalExceptionService.nodeHandleAndSaveTask)
            if (Strings.CI.equals(nextNode.getNodeType(), ApprovalNodeTypeEnum.END.name())) {
                return approvalTasks;
            }
            // TODO: 条件类型的节点需要继续往下取直到获取到审批类型的节点
            if (nextNode instanceof ApprovalNodeApproverResponse nodeApprover) {
                // TODO: 审批类型需要考虑多人审批场景, 追加任务待办
                ApprovalTask approvalTask = new ApprovalTask();
                approvalTask.setId(IDGenerator.nextStr());
                approvalTask.setNodeId(nodeApprover.getId());
                approvalTask.setInstanceId(currentTask.getInstanceId());
                approvalTask.setStatus(ApprovalStatus.APPROVING.name());
                approvalTask.setType(ApprovalTaskType.NL.name());
                approvalTask.setCreateUser(currentUserId);
                approvalTask.setCreateTime(System.currentTimeMillis());
                approvalTask.setUpdateUser(currentUserId);
                approvalTask.setUpdateTime(System.currentTimeMillis());
                approvalTasks.add(approvalTask);
            }
        }

        return approvalTasks;
    }


    /**
     * 批量驳回
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void batchReject(ApprovalActionBatchRequest request, String userId, String orgId) {
        List<ApprovalTask> approvalTasks = approvalTaskMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(approvalTasks)) {
            throw new GenericException("审批任务不存在!");
        }
        /*
         * 驳回: 当前任务所属节点最终执行状态为驳回, 中断审批流程
         *     TODO: 特殊场景: 节点多人审批, 会签及依次审批时直接整体走驳回逻辑; 反过来如果是或签, 则需要节点下审批任务都为驳回状态才整体走驳回逻辑
         */
        List<String> instanceIds = approvalTasks.stream().map(ApprovalTask::getInstanceId).toList();
        List<ApprovalInstance> approvalInstances = approvalInstanceMapper.selectByIds(instanceIds);
        Map<String, ApprovalInstance> instanceMaps = approvalInstances.stream().collect(Collectors.toMap(ApprovalInstance::getId, Function.identity()));
        List<LogDTO> logs = new ArrayList<>();
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtApprovalTaskMapper taskMapper = sqlSession.getMapper(ExtApprovalTaskMapper.class);
        approvalTasks.stream().forEach(approvalTask -> {
            approvalTask.setAction(ApprovalAction.REJECT.name());
            approvalTask.setStatus(ApprovalStatus.UNAPPROVED.name());
            approvalTask.setUpdateUser(userId);
            approvalTask.setUpdateTime(System.currentTimeMillis());
            taskMapper.updateTaskById(approvalTask);

            saveApprovalRecord(approvalTask, request.getRejectReason(), request.getAttachmentIds(), userId, orgId);
            if (instanceMaps.containsKey(approvalTask.getInstanceId())) {
                ApprovalInstance approvalInstance = instanceMaps.get(approvalTask.getInstanceId());
                finishApprovalInstance(approvalInstance, ApprovalStatus.REVOKED.name(), userId);


                String resourceName = approvalResourceService.selectBusinessName(FormKey.valueOf(approvalInstance.getType()), approvalInstance.getResourceId());
                LogDTO logDTO = new LogDTO(orgId, approvalInstance.getResourceId(), userId, LogType.APPROVAL, request.getModule(), resourceName);
                logDTO.setModifiedValue(Translator.get("reject_approval"));
                logs.add(logDTO);
            }
        });
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        logService.batchAdd(logs);
    }
}
