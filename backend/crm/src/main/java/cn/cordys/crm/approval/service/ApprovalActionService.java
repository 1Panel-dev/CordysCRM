package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.AddSignSortInfo;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
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
	private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;
	@Resource
	private BaseMapper<ApprovalAddSignTask> approvalAddSignTaskMapper;
	@Resource
	private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
	@Resource
	private BaseMapper<ApprovalRecord> approvalRecordMapper;
	@Resource
	private AttachmentService attachmentService;
	@Resource
	private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
	@Resource
	private LogService logService;
	@Resource
	private SqlSessionFactory sqlSessionFactory;
	@Resource
	private ApprovalResourceService approvalResourceService;

	public static final Long DEFAULT_SIGN_SORT_STEP = 100L;

	/**
	 * 加签
	 *
	 * @param request 加签参数
	 * @param userId  当前用户
	 * @param orgId   当前组织
	 */
	public void sign(ApprovalAddSignRequest request, String userId, String orgId) {
		// 审批流是否允许加签
		ApprovalFlowVersion flowVersion = getFlowVersionOfInstanceId(request.getInstanceId());
		if (flowVersion == null || !flowVersion.getAllowAddSign()) {
			throw new GenericException(Translator.get("no.operation.permission"));
		}
		ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
		// 加签操作的待办任务
		ApprovalTask appendActionTask = appendSignTask(request, userId);
		ApprovalAddSignTask addSignTask = saveAddSignTask(request, appendActionTask.getId());
		// 刷新被加签任务状态 && 插入审批记录
		saveActionTask(request, ApprovalAction.SIGN, userId, orgId, ApprovalAddSignType.valueOf(request.getType()));
		// 之后加签(多人或签), 需要刷新实例当前审批节点
		if (ApprovalAddSignType.valueOf(request.getType()) == ApprovalAddSignType.AFTER && isMultiAnyMode(appendActionTask.getNodeId(), userId, orgId)) {
			ApprovalNodeResponse nextNode = approvalFlowService.getTaskNextApproverNode(appendActionTask, instance);
			instance.setCurrentNodeId(nextNode.getId());
			approvalInstanceMapper.updateById(instance);
		}
		// 保存加签附件
		saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), addSignTask.getId(), userId, orgId);
	}

	/**
	 * 回退
	 *
	 * @param request 回退参数
	 * @param userId  当前用户
	 * @param orgId   当前组织
	 */
	public void back(ApprovalReturnBackRequest request, String userId, String orgId) {
		ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
		// 追加退回操作的待办任务 && 保存退回记录
		appendBackTasks(request, instance.getSubmitterId(), orgId);
		ApprovalReturnBackRecord backRecord = saveBackRecord(request, userId);
		// 保存执行任务
		saveActionTask(request, ApprovalAction.BACK, userId, orgId, null);
		// 保存退回附件
		saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), backRecord.getId(), userId, orgId);
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
		ApprovalTask currentTask = saveActionTask(request, ApprovalAction.APPROVE, currentUserId, currentOrgId, null);
		approvedProcess(currentTask, currentUserId, currentOrgId);
	}

	/**
	 * 驳回
	 *
	 * @param request       驳回参数
	 * @param currentUserId 当前用户
	 * @param currentOrgId  当前组织
	 */
	public void reject(ApprovalActionRequest request, String currentUserId, String currentOrgId) {
		ApprovalTask currentTask = saveActionTask(request, ApprovalAction.REJECT, currentUserId, currentOrgId, null);
		rejectProcess(currentTask, currentUserId, currentOrgId);
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
	private ApprovalAddSignTask saveAddSignTask(ApprovalAddSignRequest request, String taskId) {
		// 计算加签信息
		AddSignSortInfo signSortInfo = calculateAddSignSort(request.getId(), request.getType());
		ApprovalAddSignTask approvalAddSignTask = new ApprovalAddSignTask();
		approvalAddSignTask.setId(IDGenerator.nextStr());
		approvalAddSignTask.setTaskId(taskId);
		approvalAddSignTask.setSignTaskId(request.getId());
		approvalAddSignTask.setType(request.getType());
		approvalAddSignTask.setComment(request.getComment());
		// 设置扩展字段
		approvalAddSignTask.setRootTaskId(signSortInfo.getRootTaskId());
		approvalAddSignTask.setSort(signSortInfo.getSort());
		approvalAddSignTasMapper.insert(approvalAddSignTask);
		return approvalAddSignTask;
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
		approvalTask.setType(ApprovalTaskType.SN.name());
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
	 * @param currentNodeId 当前节点ID
	 * @param instanceId    审批实例ID
	 * @param userId        当前用户ID
	 * @param currentOrgId  当前组织ID
	 * @param taskType      任务类型
	 */
	public void saveNodeApproverTasks(String currentNodeId, String instanceId, String userId, String currentOrgId, String taskType) {
		List<ApprovalTask> approvalTasks = getNodeApproverTasks(currentNodeId, instanceId, userId, currentOrgId, taskType);
		if (CollectionUtils.isNotEmpty(approvalTasks)) {
			approvalTaskMapper.batchInsert(approvalTasks);
		}
	}

	/**
	 * 获取节点审批任务
	 *
	 * @param currentNodeId 当前节点ID
	 * @param instanceId    审批实例ID
	 * @param userId        当前用户ID
	 * @param currentOrgId  当前组织ID
	 * @param taskType      任务类型
	 * @return 待办任务集合
	 */
	private List<ApprovalTask> getNodeApproverTasks(String currentNodeId, String instanceId, String userId, String currentOrgId, String taskType) {
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
		return approvalTasks;
	}

	private List<ApprovalTask> getNodeApproverTasks(ApprovalNodeApproverResponse approverNode, String instanceId, String userId, String currentOrgId, String taskType) {
		List<ApprovalTask> approvalTasks = new ArrayList<>();
		List<User> approvers = approvalFlowService.getApprovalNodeMultiApprover(approverNode, userId, currentOrgId);
		if (Strings.CI.equals(approverNode.getMultiApproverMode(), MultiApproverModeEnum.SEQUENTIAL.name()) || approvers.size() == 1) {
			// 单人或者依次审批, 只会产生一条待办任务
			User approverUser = approvers.getFirst();
			approvalTasks.add(buildTask(approverNode.getId(), instanceId, approverUser.getId(), taskType, userId));
		} else {
			// 多人审批, 且为会签或签方式
			approvers.forEach(approver -> {
				ApprovalTask approvalTask = buildTask(approverNode.getId(), instanceId, approver.getId(), taskType, userId);
				approvalTasks.add(approvalTask);
			});
		}
		return approvalTasks;
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
	 * @param instance     审批实例
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
	 * 同意操作执行
	 *
	 * @param currentTask 当前任务
	 * @param currentUserId 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	private void approvedProcess(ApprovalTask currentTask, String currentUserId, String currentOrgId) {
		// 加签类型的待办任务
		appendProcessSignTask(currentTask, currentUserId);

		// 多人依次审批类型的待办
		ApprovalNodeApprover nodeApprover = getNodeApprover(currentTask.getNodeId());
		if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.SEQUENTIAL) {
			// 依次审批, 如果存在审批中任务则跳过
			LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(ApprovalTask::getNodeId, currentTask.getNodeId())
					.eq(ApprovalTask::getInstanceId, currentTask.getInstanceId())
					.eq(ApprovalTask::getStatus, ApprovalStatus.APPROVING.name());
			List<ApprovalTask> approvingTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
			if (approvingTasks.isEmpty()) {
				User nextUser = getMultiSeqNextOne(currentTask.getNodeId(), currentTask.getInstanceId(), currentOrgId);
				if (nextUser != null) {
					ApprovalTask multiSeqNextTask = buildTask(currentTask.getNodeId(), currentTask.getInstanceId(), nextUser.getId(), ApprovalTaskType.NL.name(), currentUserId);
					approvalTaskMapper.insert(multiSeqNextTask);
				}
			}
		}

		// 节点状态流转类型的待办
		List<ApprovalTask> approvalTasks = new ArrayList<>();
		ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
		boolean multiNode = approvalFlowService.isCurrentNodeMultiApprover(currentTask.getNodeId(), instance.getSubmitterId(), currentOrgId);
		if (!multiNode || isCurrentMultiNodeApproved(currentTask.getNodeId(), currentTask.getInstanceId())) {
			// 单人审批或者多人审批但节点流转通过
			ApprovalNodeResponse nextNode = approvalFlowService.getTaskNextApproverNode(currentTask, instance);
			if (nextNode instanceof ApprovalNodeApproverResponse approverNode) {
				List<ApprovalTask> approvedTasks = getNodeApproverTasks(approverNode, currentTask.getInstanceId(), instance.getSubmitterId(), currentOrgId, ApprovalTaskType.NL.name());
				approvalTasks.addAll(approvedTasks);
			}
			instance.setCurrentNodeId(nextNode.getId());
			approvalInstanceMapper.updateById(instance);
			if (ApprovalNodeTypeEnum.valueOf(nextNode.getNodeType()) == ApprovalNodeTypeEnum.END) {
				finishApprovalInstance(instance, ApprovalStatus.APPROVED.name(), currentUserId);
			}
		}

		// 批量插入待办任务
		if (CollectionUtils.isNotEmpty(approvalTasks)) {
			approvalTaskMapper.batchInsert(approvalTasks);
		}
	}

	/**
	 * 同意操作执行
	 *
	 * @param currentTask 当前任务
	 * @param currentUserId 当前用户ID
	 * @param currentOrgId 当前组织ID
	 */
	private void rejectProcess(ApprovalTask currentTask, String currentUserId, String currentOrgId) {
		ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
		boolean multiNode = approvalFlowService.isCurrentNodeMultiApprover(currentTask.getNodeId(), instance.getSubmitterId(), currentOrgId);
		boolean nodeRejected = isCurrentMultiNodeRejected(currentTask.getNodeId(), currentTask.getInstanceId());
		if (!nodeRejected) {
			// 多人审批驳回但节点尚未流转失败, 需要发送加签待办
			appendProcessSignTask(currentTask, currentUserId);
		}
		if (!multiNode || nodeRejected) {
			// 单人审批或者多人审批但节点流转失败, 实例直接驳回结束
			finishApprovalInstance(instance, ApprovalStatus.REVOKED.name(), currentUserId);
		}
	}

	private void appendProcessSignTask(ApprovalTask currentTask, String currentUserId) {
		if (Strings.CI.equals(currentTask.getType(), ApprovalTaskType.SN.name())) {
			// 加签任务执行, 需要获取同一加签链路的下一个待办任务
			ApprovalTask nextTask = getNextAddSignTask(currentTask.getId());
			if (nextTask != null && ApprovalStatus.valueOf(nextTask.getStatus()) == ApprovalStatus.PENDING) {
				ApprovalTask signAppendTask = copyEmptyTask(nextTask, currentUserId);
				approvalTaskMapper.insert(signAppendTask);
			}
		}
	}


	/**
	 * 执行审批任务
	 *
	 * @param request       执行参数
	 * @param action        执行操作
	 * @param currentUserId 当前用户ID
	 * @return 执行任务
	 */
	private ApprovalTask saveActionTask(ApprovalActionRequest request, ApprovalAction action, String currentUserId, String currentOrgId, ApprovalAddSignType signType) {
		// 保存执行的任务及记录
		ApprovalTask currentTask = getTaskById(request.getId());
		switch (action) {
			case ApprovalAction.APPROVE: {
				currentTask.setAction(ApprovalAction.APPROVE.name());
				currentTask.setStatus(ApprovalStatus.APPROVED.name());
				break;
			}
			case ApprovalAction.REJECT: {
				currentTask.setAction(ApprovalAction.REJECT.name());
				currentTask.setStatus(ApprovalStatus.UNAPPROVED.name());
				break;
			}
			case ApprovalAction.BACK: {
				currentTask.setAction(ApprovalAction.BACK.name());
				break;
			}
			case ApprovalAction.SIGN: {
				if (signType == ApprovalAddSignType.BEFORE) {
					currentTask.setStatus(ApprovalStatus.PENDING.name());
					currentTask.setAction(ApprovalAction.SIGN.name());
				} else {
					currentTask.setAction(ApprovalAction.APPROVE.name());
					currentTask.setStatus(ApprovalStatus.APPROVED.name());
				}
				break;
			}
			default: {

			}
		}
		currentTask.setUpdateUser(currentUserId);
		currentTask.setUpdateTime(System.currentTimeMillis());
		approvalTaskMapper.updateById(currentTask);

		// 退回, 之前加签操作不产生执行记录
		if (action != ApprovalAction.BACK && signType != ApprovalAddSignType.BEFORE) {
			saveApprovalRecord(currentTask, request.getComment(), request.getAttachmentIds(), currentUserId, currentOrgId);
		}
		return currentTask;
	}

	/**
	 * 获取审批节点
	 *
	 * @param nodeId 节点ID
	 * @return 审批节点
	 */
	private ApprovalNodeApprover getNodeApprover(String nodeId) {
		return approvalNodeApproverMapper.selectByPrimaryKey(nodeId);
	}

	/**
	 * 判断当前节点是否为多人或签
	 * @param nodeId 当前节点ID
	 * @return 是否为多人或签
	 */
	private boolean isMultiAnyMode(String nodeId, String userId, String orgId) {
		boolean multi = approvalFlowService.isCurrentNodeMultiApprover(nodeId, userId, orgId);
		ApprovalNodeApprover nodeApprover = getNodeApprover(nodeId);
		return multi && Strings.CI.equals(nodeApprover.getMultiApproverMode(), MultiApproverModeEnum.ANY.name());
	}

	/**
	 * 加签时计算排序和链路信息
	 *
	 * @param sourceTaskId 被加签的任务ID（即当前用户的任务ID）
	 * @param addSignType  加签方式 BEFORE / AFTER
	 * @return 包含 rootTaskId, sort 的计算结果
	 */
	private AddSignSortInfo calculateAddSignSort(String sourceTaskId, String addSignType) {
		// 查询被加签任务是否有加签记录
		ApprovalAddSignTask signCriteria = new ApprovalAddSignTask();
		signCriteria.setTaskId(sourceTaskId);
		ApprovalAddSignTask sourceAddSignTask = approvalAddSignTaskMapper.selectOne(signCriteria);

		AddSignSortInfo signInfo = new AddSignSortInfo();
		if (sourceAddSignTask == null) {
			/*
			 * 普通任务上加签，rootTaskId就是被加签的任务ID
			 */
			signInfo.setRootTaskId(sourceTaskId);
			signInfo.setSort(DEFAULT_SIGN_SORT_STEP);
		} else {
			/*
			 * 加签任务上再加签, rootTaskId继承父加签的rootTaskId
			 */
			signInfo.setRootTaskId(sourceAddSignTask.getRootTaskId());

			// 继承父加签任务的sort
			long parentSort = sourceAddSignTask.getSort() != null ? sourceAddSignTask.getSort() : 0L;
			if (ApprovalAddSignType.BEFORE.name().equalsIgnoreCase(addSignType)) {
				// BEFORE: 插在父节点之前
				signInfo.setSort(parentSort - 100);
			} else {
				// AFTER: 插在父节点之后, 需要比父节点大但比下一个任务小
				Long nextSort = getNextSortByRootTask(sourceAddSignTask.getRootTaskId(), parentSort);
				if (nextSort != null) {
					// 有下一个任务，插在中间：(parentSort + nextSort) / 2
					signInfo.setSort((parentSort + nextSort) / 2);
				} else {
					// 没有下一个任务，直接 +100
					signInfo.setSort(parentSort + 100);
				}
			}
		}

		return signInfo;
	}

	/**
	 * 获取指定根任务节点下，比当前排序值大的最小排序值（即下一个任务排序值）
	 */
	private Long getNextSortByRootTask(String rootTaskId, Long currentSort) {
		LambdaQueryWrapper<ApprovalAddSignTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalAddSignTask::getRootTaskId, rootTaskId)
				.gt(ApprovalAddSignTask::getSort, currentSort)
				.orderByAsc(ApprovalAddSignTask::getSort);
		List<ApprovalAddSignTask> approvalAddSignTasks = approvalAddSignTaskMapper.selectListByLambda(queryWrapper);
		return CollectionUtils.isEmpty(approvalAddSignTasks) ? null : approvalAddSignTasks.getFirst().getSort();
	}

	/**
	 * 获取当前加签任务的下一个待办任务 (同一加签链路)
	 * @param currentTaskId 当前加签任务ID
	 * @return 下一个待办任务
	 */
	private ApprovalTask getNextAddSignTask(String currentTaskId) {
		// 1. 查询当前任务的加签记录
		ApprovalAddSignTask currentAddSign = new ApprovalAddSignTask();
		currentAddSign.setTaskId(currentTaskId);
		currentAddSign = approvalAddSignTaskMapper.selectOne(currentAddSign);

		if (currentAddSign == null) {
			return null;
		}

		String rootTaskId = currentAddSign.getRootTaskId();
		Long currentSort = currentAddSign.getSort();
		// 2. 查询同一个根任务节点下，sort比当前任务大的记录
		LambdaQueryWrapper<ApprovalAddSignTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalAddSignTask::getRootTaskId, rootTaskId)
				.gt(ApprovalAddSignTask::getSort, currentSort)
				.orderByAsc(ApprovalAddSignTask::getSort);
		List<ApprovalAddSignTask> signTasks = approvalAddSignTaskMapper.selectListByLambda(queryWrapper);

		if (CollectionUtils.isNotEmpty(signTasks)) {
			// 3. 找到下一个加签任务，返回对应的审批任务
			return approvalTaskMapper.selectByPrimaryKey(signTasks.getFirst().getTaskId());
		}

		// 4. 没有下一个加签任务了，返回根任务（原始流程节点的任务）
		return approvalTaskMapper.selectByPrimaryKey(currentAddSign.getRootTaskId());
	}

	/**
	 * 复制一个空待办任务
	 * @param old 旧加签任务
	 * @param currentUserId 执行人
	 * @return 新待办任务
	 */
	private ApprovalTask copyEmptyTask(ApprovalTask old, String currentUserId) {
		old.setId(IDGenerator.nextStr());
		old.setAction(null);
		old.setStatus(ApprovalStatus.APPROVING.name());
		old.setCreateTime(System.currentTimeMillis());
		old.setUpdateTime(System.currentTimeMillis());
		old.setCreateUser(currentUserId);
		old.setUpdateUser(currentUserId);
		return old;
	}

	private User getMultiSeqNextOne(String nodeId, String instanceId, String currentOrgId) {
		ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(instanceId);
		List<User> approvers = approvalFlowService.getCurrentNodeMultiApprover(nodeId, instance.getSubmitterId(), currentOrgId);
		LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalTask::getNodeId, nodeId)
				.eq(ApprovalTask::getInstanceId, instanceId)
				.eq(ApprovalTask::getType, ApprovalTaskType.NL.name())
				.eq(ApprovalTask::getStatus, ApprovalStatus.APPROVED.name());
		List<ApprovalTask> approvedTask = approvalTaskMapper.selectListByLambda(queryWrapper);
		if (approvedTask.size() < approvers.size()) {
			// 依次审批, 返回下一个审批人
			return approvers.get(approvedTask.size());
		}
		return null;
	}

	/**
	 * 判断当前多人审批节点是否通过
	 * @param currentNodeId 当前节点ID
	 * @return 是否通过
	 */
	private boolean isCurrentMultiNodeApproved(String currentNodeId, String instanceId) {
		LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
				.eq(ApprovalTask::getInstanceId, instanceId);
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
		ApprovalNodeApprover nodeApprover = getNodeApprover(currentNodeId);
		if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.ANY) {
			// 或签, 只要有一个审批通过任务即可
			return approvalTasks.stream().anyMatch(task -> ApprovalStatus.APPROVED.name().equals(task.getStatus()));
		} else {
			// 会签或者依次审批, 不存在审批中的任务即可
			return approvalTasks.stream().noneMatch(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus()));
		}
	}

	/**
	 * 判断当前多人审批节点是否驳回
	 * @param currentNodeId 当前节点ID
	 * @return 是否通过
	 */
	private boolean isCurrentMultiNodeRejected(String currentNodeId, String instanceId) {
		LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
				.eq(ApprovalTask::getInstanceId, instanceId);
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
		ApprovalNodeApprover nodeApprover = getNodeApprover(currentNodeId);
		if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.ANY) {
			// 或签, 所有审批任务都为驳回才算驳回
			return approvalTasks.stream().allMatch(task -> ApprovalStatus.REVOKED.name().equals(task.getStatus()));
		} else {
			// 会签或者依次审批, 只要存在驳回即整个节点驳回
			return approvalTasks.stream().anyMatch(task -> ApprovalStatus.REVOKED.name().equals(task.getStatus()));
		}
	}

	/**
	 * 批量驳回
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
