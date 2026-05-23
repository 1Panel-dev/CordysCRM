package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.AddSignSortInfo;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalActionService {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalInstanceService approvalInstanceService;
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
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private LogService logService;
    @Resource
    private ExtApprovalTaskMapper extApprovalTaskMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    public static final Long DEFAULT_SIGN_SORT_STEP = 100L;

    /**
     * 加签
     *
     * @param request 加签参数
     * @param userId  当前用户
     * @param orgId   当前组织
     */
    public void sign(ApprovalAddSignRequest request, String userId, String orgId) {
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(request.getInstanceId());
        // 审批流是否允许加签
        ApprovalFlow approvalFlow = approvalFlowService.selectApprovalFlowByFormType(instance.getType(), orgId);
        if (approvalFlow == null || !approvalFlow.getAllowAddSign()) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
        // 刷新被加签任务状态 && 插入审批记录
        ApprovalTask currentTask = saveActionTask(request, ApprovalAction.SIGN, userId, orgId, ApprovalAddSignType.valueOf(request.getType()));
        // 加签操作的待办任务
        ApprovalTask appendActionTask = appendSignTask(request, userId, currentTask.getNodeRound());
        ApprovalAddSignTask addSignTask = saveAddSignTask(request, appendActionTask.getId());
        // 之后加签(多人或签), 需要刷新实例当前审批节点
        if (ApprovalAddSignType.valueOf(request.getType()) == ApprovalAddSignType.AFTER && isMultiAnyMode(appendActionTask.getNodeId(), userId, orgId)) {
            handlePreCcTasks(currentTask.getNodeId(), instance, userId, orgId);
            ApprovalNodeResponse nextNode = approvalFlowService.getTaskNextNode(appendActionTask, instance, orgId);
            handleNextApprovalNode(nextNode, instance, userId);
        }
        // 保存加签附件
        if (CollectionUtils.isNotEmpty(request.getAttachmentIds())) {
            saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), addSignTask.getId(), userId, orgId);
        }

        saveLogAndNotice(instance, userId, orgId, ApprovalAction.SIGN, false);
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
		backProcess(instance, request, orgId);
		ApprovalReturnBackRecord backRecord = saveBackRecord(request, instance.getId(), userId);
		// 保存执行任务
		saveActionTask(request, ApprovalAction.BACK, userId, orgId, null);
		instance.setCurrentNodeId(request.getReturnToNodeId());
		approvalInstanceMapper.updateById(instance);
		// 保存退回附件
		if (CollectionUtils.isNotEmpty(request.getAttachmentIds())) {
			saveInstanceAttachment(request.getAttachmentIds(), request.getInstanceId(), backRecord.getId(), userId, orgId);
		}
		saveLogAndNotice(instance, userId, orgId, ApprovalAction.BACK, false);
	}

    /**
     * 撤回
     *
     * @param request       撤回参数
     * @param currentUserId 当前用户
     * @param orgId         当前组织
     */
    public void revoke(ApprovalRevokeRequest request, String currentUserId, String orgId) {
        ApprovalTask currentTask = getTaskById(request.getId());
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
        // 审批流是否允许撤回
        ApprovalFlow approvalFlow = approvalFlowService.selectApprovalFlowByFormType(instance.getType(), orgId);
        if (approvalFlow == null || !approvalFlow.getAllowWithdraw()) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
        revokeProcess(currentTask, instance, orgId);
        refreshRevokeTask(currentTask, instance, currentUserId);
        saveLogAndNotice(instance, currentUserId, orgId, ApprovalAction.REVOKE, false);
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
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
        approvedProcess(instance, currentTask, currentUserId, currentOrgId);
        saveLogAndNotice(instance, currentUserId, currentOrgId, ApprovalAction.APPROVE, request.isFromToDo());
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
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(currentTask.getInstanceId());
        rejectProcess(instance, currentTask, currentUserId, currentOrgId);
        saveLogAndNotice(instance, currentUserId, currentOrgId, ApprovalAction.REJECT, request.isFromToDo());
    }

    /**
     * 批量同意
     *
     * @param request        请求参数
     * @param userId         用户ID
     * @param organizationId 组织ID
     */
    public void batchApprove(ApprovalActionBatchRequest request, String userId, String organizationId) {
        List<ApprovalTask> approvalTasks = approvalTaskMapper.selectByIds(request.getIds());
        approvalTasks.forEach(approvalTask -> approve(ApprovalActionRequest.builder().id(approvalTask.getId()).instanceId(approvalTask.getInstanceId())
                .nodeId(approvalTask.getNodeId()).approverId(approvalTask.getApproverId())
                .comment(request.getComment()).attachmentIds(request.getAttachmentIds())
                .build(), userId, organizationId));
    }

    /**
     * 批量驳回
     *
     * @param request 请求参数
     * @param userId  当前用户ID
     * @param orgId   当前组织ID
     */
    public void batchReject(ApprovalActionBatchRequest request, String userId, String orgId) {
        List<ApprovalTask> approvalTasks = approvalTaskMapper.selectByIds(request.getIds());
        approvalTasks.forEach(approvalTask -> reject(ApprovalActionRequest.builder().id(approvalTask.getId()).nodeId(approvalTask.getNodeId()).instanceId(approvalTask.getInstanceId())
                        .approverId(approvalTask.getApproverId()).comment(request.getComment()).attachmentIds(request.getAttachmentIds()).build(),
                userId, orgId));
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
	private ApprovalTask appendSignTask(ApprovalAddSignRequest request, String userId, int round) {
		ApprovalTask approvalTask = new ApprovalTask();
		BeanUtils.copyBean(approvalTask, request);
		approvalTask.setId(IDGenerator.nextStr());
		approvalTask.setNodeId(request.getNodeId());
		approvalTask.setApproverId(request.getSignApprover());
		approvalTask.setNodeRound(round);
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
		saveNodeApproverTasks(backRequest.getReturnToNodeId(), backRequest.getInstanceId(), userId, currentOrgId, ApprovalTaskType.NL.name());
	}

    /**
     * 保存退回节点信息
     *
     * @param request 退回参数
     * @param userId  当前用户ID
     * @return 退回节点信息
     */
    private ApprovalReturnBackRecord saveBackRecord(ApprovalReturnBackRequest request, String instanceId, String userId) {
        ApprovalReturnBackRecord backRecord = new ApprovalReturnBackRecord();
        backRecord.setId(IDGenerator.nextStr());
        backRecord.setInstanceId(instanceId);
        backRecord.setTaskId(request.getId());
        backRecord.setReturnToNodeId(request.getReturnToNodeId());
        backRecord.setReturnReason(request.getComment());
        backRecord.setReturnUserId(userId);
        // 每个审批实例, 只保留最新的一条退回节点记录
        approvalReturnBackRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalReturnBackRecord>().eq(ApprovalReturnBackRecord::getInstanceId, instanceId)
                .eq(ApprovalReturnBackRecord::getReturnToNodeId, request.getReturnToNodeId()));
        approvalReturnBackRecordMapper.insert(backRecord);
        return backRecord;
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
		List<ApprovalRecord> records = approvalRecordMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalRecord>().eq(ApprovalRecord::getInstanceId, currentTask.getInstanceId())
				.eq(ApprovalRecord::getTaskId, currentTask.getId()).eq(ApprovalRecord::getNodeRound, currentTask.getNodeRound()).eq(ApprovalRecord::getNodeId, currentTask.getNodeId()));
		if (CollectionUtils.isNotEmpty(records)) {
			// 撤回的任务已经产生执行记录
			if (StringUtils.isBlank(comment) && CollectionUtils.isEmpty(attachmentIds)) {
				return;
			}
			// 如果产生了新的附件和意见, 清理掉
			approvalRecordMapper.deleteByIds(records.stream().map(ApprovalRecord::getId).toList());
		}
		ApprovalRecord record = new ApprovalRecord();
		record.setId(IDGenerator.nextStr());
		record.setInstanceId(currentTask.getInstanceId());
		record.setTaskId(currentTask.getId());
		record.setNodeRound(currentTask.getNodeRound());
		record.setNodeId(currentTask.getNodeId());
		record.setComment(comment);
		record.setCreateTime(System.currentTimeMillis());
		record.setCreateUser(currentUserId);
		record.setUpdateTime(System.currentTimeMillis());
		record.setUpdateUser(currentUserId);
		approvalRecordMapper.insert(record);
		if (CollectionUtils.isNotEmpty(attachmentIds)) {
			saveInstanceAttachment(attachmentIds, currentTask.getInstanceId(), record.getId(), currentUserId, orgId);
		}
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
     * 同意操作执行
     *
     * @param currentTask   当前任务
     * @param currentUserId 当前用户ID
     * @param currentOrgId  当前组织ID
     */
    private void approvedProcess(ApprovalInstance instance, ApprovalTask currentTask, String currentUserId, String currentOrgId) {
        // 加签类型的待办任务
        appendProcessSignTask(currentTask);

		// 多人依次审批类型的待办
		List<ApprovalTask> multiSeqApprovalTasks = new ArrayList<>();
		ApprovalNodeApprover nodeApprover = getNodeApprover(currentTask.getNodeId());
		if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.SEQUENTIAL) {
			// 依次审批, 如果存在审批中任务则跳过
			LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(ApprovalTask::getNodeId, currentTask.getNodeId())
					.eq(ApprovalTask::getInstanceId, currentTask.getInstanceId())
					.eq(ApprovalTask::getStatus, ApprovalStatus.APPROVING.name()).nq(ApprovalTask::getType, ApprovalTaskType.CC);
			List<ApprovalTask> approvingTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
			if (approvingTasks.isEmpty()) {
				User nextUser = getMultiSeqNextOne(currentTask.getNodeId(), currentTask.getInstanceId(), currentOrgId);
				if (nextUser != null) {
					ApprovalTask nextTask = buildTask(currentTask.getNodeId(), currentTask.getInstanceId(), nextUser.getId(), ApprovalTaskType.NL.name(), currentUserId, currentTask.getNodeRound());
					// 如果配置审批人提审人相同跳过
					SameSubmitterActionEnum sameAction = SameSubmitterActionEnum.valueOf(nodeApprover.getSameSubmitterAction());
					if (sameAction == SameSubmitterActionEnum.SKIP && Strings.CI.equals(nextUser.getId(), instance.getSubmitterId())) {
						nextTask.setAction(ApprovalAction.APPROVE.name());
						nextTask.setStatus(ApprovalStatus.AUTO_APPROVED.name());
						approvalFlowService.saveAutoRecord(currentTask.getInstanceId(), currentTask.getNodeId(), ApprovalStatus.AUTO_APPROVED, "审批人与提交人为同一人时, 自动同意跳过", nextTask.getId(), null, false, nextTask.getNodeRound());
						// 依次审批下一位自动同意跳过, 发送下下一个位次的待办
						User nextPlusUser = getMultiSeqNextNextOne(currentTask.getNodeId(), currentTask.getInstanceId(), currentOrgId);
						if (nextPlusUser != null) {
							ApprovalTask nextNextTask = buildTask(currentTask.getNodeId(), currentTask.getInstanceId(), nextPlusUser.getId(), ApprovalTaskType.NL.name(), currentUserId, currentTask.getNodeRound());
							multiSeqApprovalTasks.add(nextNextTask);
						}
					}
					multiSeqApprovalTasks.add(nextTask);
					approvalTaskMapper.batchInsert(multiSeqApprovalTasks);
				}
			}
		}

        // 节点状态流转类型的待办
        List<ApprovalTask> approvalTasks = new ArrayList<>();
        if (isCurrentSingleNodeApproved(currentTask.getNodeId(), currentTask.getInstanceId()) || isCurrentMultiNodeApproved(currentTask.getNodeId(), currentTask.getInstanceId())) {
            // 流转之前需要发送当前节点的抄送
            handlePreCcTasks(currentTask.getNodeId(), instance, currentUserId, currentOrgId);
            // 单人审批或者多人审批但节点流转通过
            ApprovalNodeResponse nextNode = approvalFlowService.getTaskNextNode(currentTask, instance, currentOrgId);
            handleNextApprovalNode(nextNode, instance, currentUserId);
        }

        // 批量插入待办任务
        if (CollectionUtils.isNotEmpty(approvalTasks)) {
            approvalTaskMapper.batchInsert(approvalTasks);
        }
    }

    /**
     * 驳回操作执行
     *
     * @param currentTask   当前任务
     * @param currentUserId 当前用户ID
     * @param currentOrgId  当前组织ID
     */
    private void rejectProcess(ApprovalInstance instance, ApprovalTask currentTask, String currentUserId, String currentOrgId) {
        boolean multiNode = approvalFlowService.isCurrentNodeMultiApprover(currentTask.getNodeId(), instance.getSubmitterId(), currentOrgId);
        boolean nodeRejected = isCurrentMultiNodeRejected(currentTask.getNodeId(), currentTask.getInstanceId());
        if (!nodeRejected) {
            // 多人审批驳回但节点尚未流转失败, 需要发送加签待办
            appendProcessSignTask(currentTask);
        }
        if (!multiNode || nodeRejected) {
            // 单人审批或者多人审批但节点流转失败, 实例直接驳回结束
            approvalInstanceService.rejectApprovalInstance(instance, currentUserId);
            ApprovalResourceService resourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
            if (resourceService != null) {
                resourceService.updateResourceApprovalStatus(FormKey.ofKey(instance.getType()), instance.getResourceId(), instance.getApprovalStatus());
            }
        }
    }

    /**
     * 加签操作导致的后续待办任务
     *
     * @param currentTask 当前节点任务
     */
    private void appendProcessSignTask(ApprovalTask currentTask) {
        if (Strings.CI.equals(currentTask.getType(), ApprovalTaskType.SN.name())) {
            // 加签任务执行, 需要获取同一加签链路的下一个待办任务
            ApprovalTask nextTask = getNextAddSignTask(currentTask.getId());
            if (nextTask != null) {
                nextTask.setCreateTime(System.currentTimeMillis());
                nextTask.setUpdateTime(System.currentTimeMillis());
                approvalTaskMapper.insert(nextTask);
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
			case APPROVE: {
				currentTask.setAction(ApprovalAction.APPROVE.name());
				currentTask.setStatus(ApprovalStatus.APPROVED.name());
				break;
			}
			case REJECT: {
				currentTask.setAction(ApprovalAction.REJECT.name());
				currentTask.setStatus(ApprovalStatus.UNAPPROVED.name());
				break;
			}
			case BACK: {
				currentTask.setStatus(ApprovalStatus.PENDING.name());
				currentTask.setAction(ApprovalAction.BACK.name());
				break;
			}
			case SIGN: {
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
     *
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
     *
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
            // 下一个加签节点 (断开, 指向新的任务节点)
            ApprovalAddSignTask nextAddSignTask = signTasks.getFirst();
            ApprovalTask oldTask = approvalTaskMapper.selectByPrimaryKey(nextAddSignTask.getTaskId());
            ApprovalTask newTask = copyEmptyTask(oldTask);
            nextAddSignTask.setTaskId(newTask.getId());
            approvalAddSignTaskMapper.updateById(nextAddSignTask);
            return newTask;
        }

        // 4. 没有下一个加签任务了，返回根任务（原始流程节点的任务）
        ApprovalTask rootTask = approvalTaskMapper.selectByPrimaryKey(currentAddSign.getRootTaskId());
        if (ApprovalStatus.valueOf(rootTask.getStatus()) == ApprovalStatus.PENDING) {
            ApprovalTask copyRoot = copyEmptyTask(rootTask);
            extApprovalTaskMapper.moveAddSignRoot(rootTask.getId(), copyRoot.getId());
            extApprovalTaskMapper.updateRootNext(rootTask.getId(), copyRoot.getId());
            return copyRoot;
        }
        return null;
    }

    /**
     * 复制新加签待办任务
     *
     * @param old 旧加签任务
     * @return 新待办任务
     */
    private ApprovalTask copyEmptyTask(ApprovalTask old) {
        ApprovalTask newTask = BeanUtils.copyBean(new ApprovalTask(), old);
        newTask.setId(IDGenerator.nextStr());
        newTask.setAction(null);
        newTask.setStatus(ApprovalStatus.APPROVING.name());
        return newTask;
    }

    /**
     * 获取多人依次审批下一个审批人
     *
     * @param nodeId       节点ID
     * @param instanceId   实例ID
     * @param currentOrgId 组织ID
     * @return 下一个审批人
     */
    private User getMultiSeqNextOne(String nodeId, String instanceId, String currentOrgId) {
        return getMultiSeqAfterOne(nodeId, instanceId, currentOrgId, 0);
    }

    /**
     * 获取多人依次审批下下一个审批人
     *
     * @param nodeId       节点ID
     * @param instanceId   实例ID
     * @param currentOrgId 组织ID
     * @return 下一个审批人
     */
    private User getMultiSeqNextNextOne(String nodeId, String instanceId, String currentOrgId) {
        return getMultiSeqAfterOne(nodeId, instanceId, currentOrgId, 1);
    }

    /**
     * 获取多人依次审批后续审批人
     *
     * @param nodeId       节点ID
     * @param instanceId   实例ID
     * @param currentOrgId 组织ID
     * @param next         后续位次 (从0开始, 0为下一个)
     * @return 后续审批人
     */
    private User getMultiSeqAfterOne(String nodeId, String instanceId, String currentOrgId, Integer next) {
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(instanceId);
        List<User> approvers = approvalFlowService.getCurrentNodeApproverList(nodeId, instance.getSubmitterId(), currentOrgId);
        LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalTask::getNodeId, nodeId)
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getType, ApprovalTaskType.NL.name())
                .eq(ApprovalTask::getStatus, ApprovalStatus.APPROVED.name());
        List<ApprovalTask> approvedTask = approvalTaskMapper.selectListByLambda(queryWrapper);
        if (approvedTask.size() < approvers.size()) {
            // 依次审批, 返回下下一个审批人
            return approvers.get(approvedTask.size() + next);
        }
        return null;
    }

    /**
     * 获取多人依次审批当前用户下一个审批人
     *
     * @param nodeId          节点ID
     * @param instanceId      实例ID
     * @param currentOrgId    组织ID
     * @param currentApprover 当前审批人
     * @return 下一个审批人
     */
    private User getMultiSeqCurrentNextOne(String nodeId, String instanceId, String currentOrgId, String currentApprover) {
        ApprovalInstance instance = approvalInstanceMapper.selectByPrimaryKey(instanceId);
        List<User> approvers = approvalFlowService.getCurrentNodeApproverList(nodeId, instance.getSubmitterId(), currentOrgId);
        for (int i = 0; i < approvers.size(); i++) {
            if (Strings.CI.equals(approvers.get(i).getId(), currentApprover)) {
                return approvers.get(i + 1);
            }
        }
        return null;
    }

	private boolean isCurrentSingleNodeApproved(String currentNodeId, String instanceId) {
		LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
				.eq(ApprovalTask::getInstanceId, instanceId)
				.eq(ApprovalTask::getStatus, ApprovalStatus.APPROVING).nq(ApprovalTask::getType, ApprovalTaskType.CC);
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
		return approvalTasks.isEmpty();
	}

	/**
	 * 判断当前多人审批节点是否通过
	 * @param currentNodeId 当前节点ID
	 * @return 是否通过
	 */
	private boolean isCurrentMultiNodeApproved(String currentNodeId, String instanceId) {
		LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
				.eq(ApprovalTask::getInstanceId, instanceId).nq(ApprovalTask::getType, ApprovalTaskType.CC);
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
     *
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
            return approvalTasks.stream().allMatch(task -> ApprovalStatus.UNAPPROVED.name().equals(task.getStatus()));
        } else {
            // 会签或者依次审批, 只要存在驳回即整个节点驳回
            return approvalTasks.stream().anyMatch(task -> ApprovalStatus.UNAPPROVED.name().equals(task.getStatus()));
        }
    }

    /**
     * 当前多人节点是否审批中
     *
     * @param currentNodeId 当前节点ID
     * @param instanceId    实例ID
     * @return 是否审批中
     */
    private boolean isCurrentMultiNodeApproving(String currentNodeId, String instanceId) {
        LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
                .eq(ApprovalTask::getInstanceId, instanceId);
        List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
        ApprovalNodeApprover nodeApprover = getNodeApprover(currentNodeId);
        if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.ANY) {
            // 或签, 只要没有审批通过任务即可
            return approvalTasks.stream().noneMatch(task -> ApprovalStatus.APPROVED.name().equals(task.getStatus()));
        } else {
            // 会签或者依次审批, 只要存在审批中的任务即整个节点为审批中
            return approvalTasks.stream().anyMatch(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus()));
        }
    }

    /**
     * 是否当前节点不是审批中
     *
     * @param currentNodeId 当前节点ID
     * @param instance      实例
     * @param currentOrgId  当前组织ID
     * @return 是否不是审批中
     */
    private boolean isCurrentNodeNotApproving(String currentNodeId, ApprovalInstance instance, String currentOrgId) {
        boolean multiApprover = approvalFlowService.isCurrentNodeMultiApprover(currentNodeId, instance.getSubmitterId(), currentOrgId);
        if (multiApprover) {
            return !isCurrentMultiNodeApproving(currentNodeId, instance.getId());
        } else {
            LambdaQueryWrapper<ApprovalTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApprovalTask::getNodeId, currentNodeId)
                    .eq(ApprovalTask::getInstanceId, instance.getId());
            List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(queryWrapper);
            return approvalTasks.stream().noneMatch(task -> ApprovalStatus.APPROVING.name().equals(task.getStatus()));
        }
    }

	/**
	 * 退回执行
	 * @param instance 审批实例
	 * @param request 请求参数
	 * @param orgId 组织ID
	 */
	private void backProcess(ApprovalInstance instance, ApprovalReturnBackRequest request, String orgId) {
		if (ApprovalStatus.valueOf(instance.getApprovalStatus()) != ApprovalStatus.APPROVING) {
			// 非审批中, 无法进行节点退回
			throw new GenericException(Translator.get("no.back.approval"));
		}
		// 追加退回节点的待办
		appendBackTasks(request, instance.getSubmitterId(), orgId);
		// 清理后续所有执行过的节点待办轮次
		clearBackToCurrentNode(request.getReturnToNodeId(), request.getNodeId(), instance, orgId);
	}

    private void clearBackToCurrentNode(String currentNodeId, String endNodeId, ApprovalInstance instance, String orgId) {
        if (Strings.CI.equals(currentNodeId, endNodeId)) {
            return;
        }
        ApprovalNodeResponse next = approvalFlowService.getCurrentNextNode(currentNodeId, instance, orgId);
        clearCurrentNode(instance.getId(), next.getId());
        clearBackToCurrentNode(next.getId(), endNodeId, instance, orgId);
    }

	public void clearCurrentNode(String instanceId, String nodeId) {
		Integer maxRound = extApprovalInstanceMapper.getNodeRound(instanceId, nodeId);
		if (maxRound > 0) {
			/*
			 * 当前节点执行过, 假删除, 保留历史待办
			 */
			extApprovalInstanceMapper.batchClearTask(instanceId, nodeId, maxRound);
			extApprovalInstanceMapper.batchClearRecord(instanceId, nodeId, maxRound);
		}
	}

	/**
	 * 当前节点撤回
	 * @param instanceId 审批实例ID
	 * @param nodeId 节点ID
	 */
	public void revokeCurrentNode(String instanceId, String nodeId) {
		Integer maxRound = extApprovalInstanceMapper.getNodeRound(instanceId, nodeId);
		if (maxRound > 0) {
			/*
			 * 当前节点执行过
			 * 审批中 => 已撤销
			 * 待审批 => 待审批
			 * 已完成 => 已完成 (同意, 驳回)
			 */
			extApprovalInstanceMapper.revokeApprovingTask(instanceId, nodeId, maxRound);
		}
	}

    /**
     * 撤回操作执行
     *
     * @param currentTask  当前任务
     * @param instance     当前实例
     * @param currentOrgId 当前组织ID
     */
    private void revokeProcess(ApprovalTask currentTask, ApprovalInstance instance, String currentOrgId) {
        boolean multiApprover = approvalFlowService.isCurrentNodeMultiApprover(currentTask.getNodeId(), instance.getSubmitterId(), currentOrgId);
        // 撤回不涉及到流转操作, 获取下一个节点用于校验后续节点状态
        ApprovalNodeResponse nextNode = approvalFlowService.getTaskNextNode(currentTask, instance, currentOrgId);
        if (multiApprover) {
            ApprovalNodeApprover nodeApprover = getNodeApprover(currentTask.getNodeId());
            boolean nodeApproving = isCurrentMultiNodeApproving(currentTask.getNodeId(), currentTask.getInstanceId());
            if (!nodeApproving && MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.ALL) {
                // 多人会签, 但当前节点非审批中, 节点任务不允许撤回
                throw new GenericException(Translator.get("no.revoke.approval"));
            }
            if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.ANY && isCurrentNodeNotApproving(nextNode.getId(), instance, currentOrgId)) {
                // 多人或签, 但下一个节点非审批中, 节点任务不允许撤回
                throw new GenericException(Translator.get("no.revoke.approval"));
            }
            if (MultiApproverModeEnum.valueOf(nodeApprover.getMultiApproverMode()) == MultiApproverModeEnum.SEQUENTIAL) {
                // 多人依次审批, 但当前节点非审批中, 节点任务不允许撤回
                if (!nodeApproving) {
                    throw new GenericException(Translator.get("no.revoke.approval"));
                }
                User nextUser = getMultiSeqCurrentNextOne(currentTask.getNodeId(), currentTask.getInstanceId(), currentOrgId, currentTask.getApproverId());
                if (nextUser == null) {
                    throw new GenericException(Translator.get("no.revoke.approval"));
                }
                ApprovalTask taskCriteria = new ApprovalTask();
                taskCriteria.setApproverId(nextUser.getId());
                taskCriteria.setInstanceId(instance.getId());
                taskCriteria.setNodeId(currentTask.getNodeId());
                taskCriteria.setStatus(ApprovalStatus.APPROVING.name());
                ApprovalTask nextTask = approvalTaskMapper.selectOne(taskCriteria);
                if (nextTask == null) {
                    // 多人依次审批, 撤销任务的下一个审批任务已经执行, 无法撤销
                    throw new GenericException(Translator.get("no.revoke.approval"));
                }
                // 否则清理掉, 后续重新生成
                approvalTaskMapper.delete(nextTask);
            }
        } else {
            // 单人审批
            if (ApprovalNodeTypeEnum.valueOf(nextNode.getNodeType()) == ApprovalNodeTypeEnum.END) {
                // 后续节点已结束, 不允许撤回
                throw new GenericException(Translator.get("no.revoke.approval"));
            }
            if (ApprovalNodeTypeEnum.valueOf(nextNode.getNodeType()) == ApprovalNodeTypeEnum.APPROVER && isCurrentNodeNotApproving(nextNode.getId(), instance, currentOrgId)) {
                // 后续审批节点不为审批中, 不允许撤回
                throw new GenericException(Translator.get("no.revoke.approval"));
            }
        }
        // 清理后续审批节点的待办任务, 后续执行重新生成
        clearCurrentNode(instance.getId(), nextNode.getId());
    }

	/**
	 * 重置撤回的任务
	 * @param approvalTask 任务
	 * @param instance 实例
	 * @param currentUserId 当前用户ID
	 */
	private void refreshRevokeTask(ApprovalTask approvalTask, ApprovalInstance instance, String currentUserId) {
		approvalTask.setStatus(ApprovalStatus.APPROVING.name());
		approvalTask.setAction(null);
		approvalTask.setUpdateTime(System.currentTimeMillis());
		approvalTask.setUpdateUser(currentUserId);
		approvalTaskMapper.updateById(approvalTask);
		instance.setCurrentNodeId(approvalTask.getNodeId());
		approvalInstanceMapper.updateById(instance);
	}

    private void handlePreCcTasks(String currentNodeId, ApprovalInstance instance, String currentUserId, String currentOrgId) {
        List<User> ccList = approvalFlowService.getCurrentNodeCcList(currentNodeId, instance.getSubmitterId(), currentOrgId);
        List<ApprovalTask> ccTasks = getNodeCcTasks(currentNodeId, ccList.stream().map(User::getId).toList(), instance.getId(), currentUserId);
        if (CollectionUtils.isNotEmpty(ccTasks)) {
            approvalTaskMapper.batchInsert(ccTasks);
        }
    }

    /**
     * 处理下一个节点
     *
     * @param node          下一个节点
     * @param instance      审批实例
     * @param currentUserId 当前用户ID
     */
    private void handleNextApprovalNode(ApprovalNodeResponse node, ApprovalInstance instance, String currentUserId) {
        // 更新审批实例当前节点, 插入待办和抄送任务
        instance.setCurrentNodeId(node.getId());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setUpdateTime(System.currentTimeMillis());
        instance.setUpdateUser(currentUserId);
        if (ApprovalNodeTypeEnum.valueOf(node.getNodeType()) == ApprovalNodeTypeEnum.END) {
            instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
            instance.setApprovalTime(System.currentTimeMillis());
        }
        if (ApprovalNodeTypeEnum.valueOf(node.getNodeType()) == ApprovalNodeTypeEnum.EXCEPTION) {
            instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
            instance.setApprovalTime(System.currentTimeMillis());
        }
        approvalInstanceMapper.updateById(instance);
        ApprovalResourceService resourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        if (resourceService != null) {
            resourceService.updateResourceApprovalStatus(FormKey.ofKey(instance.getType()), instance.getResourceId(), instance.getApprovalStatus());
        }
        if (ApprovalNodeTypeEnum.valueOf(node.getNodeType()) == ApprovalNodeTypeEnum.APPROVER) {
            List<ApprovalTask> approvalTasks = getNextNodeApproverTasks((ApprovalNodeApproverResponse) node, instance.getId(), currentUserId, ApprovalTaskType.NL.name(), instance.getSubmitterId());
            if (CollectionUtils.isNotEmpty(approvalTasks)) {
                approvalTaskMapper.batchInsert(approvalTasks);
            }
        }
    }

	/**
	 * 获取审批节点待办任务
	 * @param approverNode 审批节点
	 * @param instanceId 实例ID
	 * @param userId 当前用户ID
	 * @param taskType 任务类型
	 * @return 待办任务集合
	 */
	public List<ApprovalTask> getNextNodeApproverTasks(ApprovalNodeApproverResponse approverNode, String instanceId, String userId, String taskType, String submitter) {
		List<ApprovalTask> approvalTasks = new ArrayList<>();
		if (approverNode == null) {
			return approvalTasks;
		}
		Integer nextRound = extApprovalInstanceMapper.getNextNodeRound(instanceId, approverNode.getId());
		if (CollectionUtils.isNotEmpty(approverNode.getApproverList())) {
			MultiApproverModeEnum multiMode = MultiApproverModeEnum.valueOf(approverNode.getMultiApproverMode());
			SameSubmitterActionEnum sameAction = SameSubmitterActionEnum.valueOf(approverNode.getSameSubmitterAction());
			if (sameAction == SameSubmitterActionEnum.SKIP) {
				// 如果配置了审批人和提审人相同, 自动跳过 (会签, 依次审批 生成的提审人待办需直接同意, 或签和单人审批已在流程执行的时候处理过)
				switch (multiMode) {
					case SEQUENTIAL -> {
						// 依次审批只需要发送第一个人的待办
						String firstApprover = approverNode.getApproverList().get(0);
						ApprovalTask firstTask = buildTask(approverNode.getId(), instanceId, firstApprover, taskType, userId, nextRound);
						if (Strings.CI.equals(firstApprover, submitter)) {
							// 自动跳过该审批人
							firstTask.setAction(ApprovalAction.APPROVE.name());
							firstTask.setStatus(ApprovalStatus.AUTO_APPROVED.name());
							approvalFlowService.saveAutoRecord(instanceId, approverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人与提交人为同一人时, 自动同意跳过", firstTask.getId(), null, false);
							// 第一个需要自动同意跳过, 发送第二个人待办
							String secondApprover = approverNode.getApproverList().get(1);
							if (StringUtils.isNotBlank(secondApprover)) {
								ApprovalTask secondTask = buildTask(approverNode.getId(), instanceId, secondApprover, taskType, userId, nextRound);
								approvalTasks.add(secondTask);
							}
						}
						approvalTasks.add(firstTask);
					}
					case ALL ->
						// 如果是会签, 需要发送所有审批人的待办
						approverNode.getApproverList().forEach(approver -> {
							ApprovalTask approvalTask = buildTask(approverNode.getId(), instanceId, approver, taskType, userId, nextRound);
							if (Strings.CI.equals(approver, submitter)) {
								// 自动跳过该审批人
								approvalTask.setAction(ApprovalAction.APPROVE.name());
								approvalTask.setStatus(ApprovalStatus.AUTO_APPROVED.name());
								approvalFlowService.saveAutoRecord(instanceId, approverNode.getId(), ApprovalStatus.AUTO_APPROVED, "审批人与提交人为同一人时, 自动同意跳过", approvalTask.getId(), null, false);
							}
							approvalTasks.add(approvalTask);
						});
					default -> {

                    }
                }
            } else {
                // 并未配置自动跳过的逻辑, 正常逻辑
                if (multiMode == MultiApproverModeEnum.SEQUENTIAL) {
                    // 依次审批只需要发送第一个人的待办
                    String firstApprover = approverNode.getApproverList().getFirst();
                    ApprovalTask firstTask = buildTask(approverNode.getId(), instanceId, firstApprover, taskType, userId, nextRound);
                    approvalTasks.add(firstTask);
                } else {
                    // 会签, 或签需要发送所有待办
                    approverNode.getApproverList().forEach(approver -> {
                        ApprovalTask approvalTask = buildTask(approverNode.getId(), instanceId, approver, taskType, userId, nextRound);
                        approvalTasks.add(approvalTask);
                    });
                }
            }
        }
        return approvalTasks;
    }

    public List<ApprovalTask> getNodeCcTasks(String nodeId, List<String> ccList, String instanceId, String userId) {
        List<ApprovalTask> approvalTasks = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ccList)) {
            Integer nextRound = extApprovalInstanceMapper.getNextNodeRound(instanceId, nodeId);
            ccList.forEach(cc -> {
                ApprovalTask approvalTask = buildTask(nodeId, instanceId, cc, ApprovalTaskType.CC.name(), userId, nextRound - 1);
                approvalTasks.add(approvalTask);
            });
        }
        return approvalTasks;
    }

    /**
     * 生成待办任务
     *
     * @param nodeId        节点ID
     * @param instanceId    实例ID
     * @param approverId    审批人ID
     * @param taskType      任务类型
     * @param currentUserId 当前用户ID
     * @param round         任务轮次
     * @return 待办任务
     */
    public ApprovalTask buildTask(String nodeId, String instanceId, String approverId, String taskType, String currentUserId, Integer round) {
        ApprovalTask approvalTask = new ApprovalTask();
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setNodeId(nodeId);
        approvalTask.setNodeRound(round);
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
        List<User> approvers = approvalFlowService.getCurrentNodeApproverList(currentNodeId, userId, currentOrgId);
        Integer nextRound = extApprovalInstanceMapper.getNextNodeRound(instanceId, currentNodeId);
        if (Strings.CI.equals(approvalNodeApprover.getMultiApproverMode(), MultiApproverModeEnum.SEQUENTIAL.name()) || approvers.size() == 1) {
            // 单人或者依次审批, 只会产生一条待办任务
            User approverUser = approvers.getFirst();
            approvalTasks.add(buildTask(currentNodeId, instanceId, approverUser.getId(), taskType, userId, nextRound));
        } else {
            // 多人审批, 且为会签或签方式
            approvers.forEach(approver -> {
                ApprovalTask approvalTask = buildTask(currentNodeId, instanceId, approver.getId(), taskType, userId, nextRound);
                approvalTasks.add(approvalTask);
            });
        }
        return approvalTasks;
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
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(approvalTasks)) {
            approvalTaskMapper.batchInsert(approvalTasks);
        }
    }


    /**
     * 保存审批执行的日志和消息通知
     *
     * @param instance 审批实例
     * @param userId   当前用户ID
     * @param orgId    当前组织ID
     */
    private void saveLogAndNotice(ApprovalInstance instance, String userId, String orgId, ApprovalAction action, boolean fromToDO) {
        // 日志
        ApprovalResourceService resourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        if (resourceService != null) {
            LogDTO logDTO = new LogDTO(orgId, instance.getResourceId(), userId, LogType.APPROVAL, fromToDO ? LogModule.WORKBENCH : getLogModuleOfFormKey(FormKey.ofKey(instance.getType())),
                    resourceService.getInstanceResourceName(FormKey.ofKey(instance.getType()), instance.getResourceId()));
            logDTO.setModifiedValue(Translator.get(StringUtils.lowerCase(action.name())));
            logService.add(logDTO);
        }
        // 结束状态的数据发送消息通知
        if (StringUtils.isBlank(instance.getApprovalStatus()) || StringUtils.isBlank(instance.getSubmitterId())) {
            return;
        }
        FormKey formKey = FormKey.ofKey(instance.getType());
        if (formKey == null) {
            return;
        }
        ApprovalStatus approvalStatus = ApprovalStatus.valueOf(instance.getApprovalStatus());
        if (approvalStatus != ApprovalStatus.APPROVED && approvalStatus != ApprovalStatus.UNAPPROVED) {
            return;
        }
        String resourceName = resourceService == null ? null :
                resourceService.getInstanceResourceName(formKey, instance.getResourceId());
        String state = approvalStatus == ApprovalStatus.APPROVED
                ? Translator.get("contract.approval_status.approved")
                : Translator.get("contract.approval_status.unapproved");
        String module;
        String event;
        switch (formKey) {
            case QUOTATION -> {
                module = NotificationConstants.Module.OPPORTUNITY;
                event = NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL;
            }
            case CONTRACT -> {
                module = NotificationConstants.Module.CONTRACT;
                event = NotificationConstants.Event.CONTRACT_APPROVAL;
            }
            case ORDER -> {
                module = NotificationConstants.Module.CONTRACT;
                event = NotificationConstants.Event.ORDER_APPROVAL;
            }
            case INVOICE -> {
                module = NotificationConstants.Module.CONTRACT;
                event = NotificationConstants.Event.INVOICE_APPROVAL;
            }
            default -> {
                return;
            }
        }

        commonNoticeSendService.sendNotice(
                module,
                event,
                Map.of("name", StringUtils.defaultString(resourceName), "state", state),
                userId,
                orgId,
                List.of(instance.getSubmitterId()),
                true
        );

    }

    /**
     * 表单类型 => 日志模块
     *
     * @param formKey 表单Key
     * @return 日志模块
     */
    private String getLogModuleOfFormKey(FormKey formKey) {
        if (formKey == null) {
            return null;
        }
        switch (formKey) {
            case QUOTATION -> {
                return LogModule.OPPORTUNITY_QUOTATION;
            }
            case CONTRACT -> {
                return LogModule.CONTRACT_INDEX;
            }
            case INVOICE -> {
                return LogModule.CONTRACT_INVOICE;
            }
            case ORDER -> {
                return LogModule.ORDER_INDEX;
            }
            default -> {
                return null;
            }
        }
    }
}
