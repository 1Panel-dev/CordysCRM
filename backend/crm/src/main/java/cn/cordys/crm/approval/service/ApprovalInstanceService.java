package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.constants.MultiApproverModeEnum;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.ApprovalCcNode;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.UserSimple;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApprovalInstanceService {

	@Resource
	private BaseMapper<ApprovalInstance> approvalInstanceMapper;
	@Resource
	private BaseMapper<ApprovalTask> approvalTaskMapper;
	@Resource
	private BaseMapper<ApprovalAddSignTask> approvalAddSignTaskMapper;
	@Resource
	private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;
	@Resource
	private BaseMapper<ApprovalRecord> approvalRecordMapper;
	@Resource
	private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
	@Resource
	private BaseMapper<Attachment> attachmentMapper;
	@Resource
	private BaseMapper<ApprovalFlowVersion> flowVersionMapper;
	@Resource
	private AttachmentService attachmentService;
	@Resource
	private UserExtendService userExtendService;
	@Resource
	private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
	@Resource
	private BaseMapper<ApprovalNode> approvalNodeMapper;
	@Resource
	@Lazy
	private ApprovalFlowService approvalFlowService;

	/**
	 * 获取资源最新审批实例详情
	 * @param resourceId 资源ID
	 * @return 审批实例详情
	 */
	public ApprovalInstanceDetail getLatestApprovalInstanceDetail(String resourceId, String currentOrgId) {
		ApprovalInstance latestInstance = getLatestInstance(resourceId);
		if (latestInstance == null) {
			return null;
		}
		Map<String, UserSimple> simpleUserMap = userExtendService.getAllUserSimpleMap();
		ApprovalInstanceDetail instanceDetail = BeanUtils.copyBean(new ApprovalInstanceDetail(), latestInstance);
		if (simpleUserMap.containsKey(instanceDetail.getSubmitterId())) {
			instanceDetail.setSubmitAvatar(simpleUserMap.get(instanceDetail.getSubmitterId()).getAvatar());
			instanceDetail.setSubmitter(simpleUserMap.get(instanceDetail.getSubmitterId()).getName());
		}
		List<ApprovalTask> tasks = getAllTasks(latestInstance);
		List<ApprovalRecord> records = getAllRecords(latestInstance);
		Map<String, List<ApprovalAddSignTask>> addSignMap = getAddSignMap(tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.NL ||
				ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.BK).map(ApprovalTask::getId).toList());
		Map<String, List<Attachment>> elementAttachmentsMap = queryAttachments(records);
		instanceDetail.setNodes(buildApprovalRecordNodeList(latestInstance, tasks, records, addSignMap, elementAttachmentsMap, simpleUserMap, currentOrgId));
		return instanceDetail;
	}

	/**
	 * 获取资源最新审批实例
	 */
	public ApprovalInstance getLatestInstance(String resourceId) {
		LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ApprovalInstance::getResourceId, resourceId);
		List<ApprovalInstance> list = approvalInstanceMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(list)) {
			throw new GenericException(Translator.get("no.approval.instance"));
		}
		return CollectionUtils.isEmpty(list) ? null : list.stream().sorted(Comparator.comparing(ApprovalInstance::getSubmitTime)).toList().getLast();
	}

	/**
	 * 获取所有的任务列表
	 */
	private List<ApprovalTask> getAllTasks(ApprovalInstance latestInstance) {
		LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId());
		return approvalTaskMapper.selectListByLambda(wrapper);
	}

	/**
	 * 获取所有的任务列表
	 */
	private Map<String, List<ApprovalAddSignTask>> getAddSignMap(List<String> taskIds) {
		LambdaQueryWrapper<ApprovalAddSignTask> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(ApprovalAddSignTask::getRootTaskId, taskIds);
		List<ApprovalAddSignTask> approvalAddSignTasks = approvalAddSignTaskMapper.selectListByLambda(wrapper);
		return approvalAddSignTasks.stream().collect(Collectors.groupingBy(ApprovalAddSignTask::getSignTaskId));
	}

	/**
	 * 获取所有的记录列表
	 */
	private List<ApprovalRecord> getAllRecords(ApprovalInstance latestInstance) {
		LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ApprovalRecord::getInstanceId, latestInstance.getId());
		return approvalRecordMapper.selectListByLambda(wrapper);
	}

	/**
	 * 查询附件信息并按节点ID分组
	 */
	private Map<String, List<Attachment>> queryAttachments(List<ApprovalRecord> records) {
		List<String> elementIds = new ArrayList<>(records.stream().map(ApprovalRecord::getId).toList());
		if (CollectionUtils.isEmpty(elementIds)) {
			return Map.of();
		}
		List<ApprovalInstanceAttachment> attachments = approvalInstanceAttachmentMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalInstanceAttachment>().in(ApprovalInstanceAttachment::getElementId, elementIds));
		if (CollectionUtils.isEmpty(attachments)) {
			return Map.of();
		}

		List<String> attachmentIds = attachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).distinct().toList();
		List<Attachment> attachmentList = attachmentMapper.selectByIds(attachmentIds);
		Map<String, Attachment> attachmentMap = attachmentList.stream().collect(Collectors.toMap(Attachment::getId, a -> a, (v1, v2) -> v1));

		return attachments.stream().collect(Collectors.groupingBy(ApprovalInstanceAttachment::getElementId,
				Collectors.mapping(a -> attachmentMap.get(a.getAttachmentId()), Collectors.toList())));
	}

	/**
	 * 构建审批记录节点列表
	 * @param tasks 审批任务列表
	 * @param records 记录列表
	 * @param addSignTaskMap 加签任务集合
	 * @param attachmentMap 附件集合
	 * @param simpleUserMap 用户信息集合
	 * @return 审批记录节点列表
	 */
	private List<ApprovalRecordNode> buildApprovalRecordNodeList(ApprovalInstance instance, List<ApprovalTask> tasks, List<ApprovalRecord> records, Map<String, List<ApprovalAddSignTask>> addSignTaskMap,
										 						 Map<String, List<Attachment>> attachmentMap, Map<String, UserSimple> simpleUserMap, String currentOrgId) {
		List<ApprovalRecordNode> processedApprovalNodes = buildProcessedApprovalNodes(tasks, records, addSignTaskMap, attachmentMap, simpleUserMap);
		List<ApprovalRecordNode> pendingApprovalNodes = buildPendingApprovalNodes(instance, simpleUserMap, currentOrgId);
		List<ApprovalRecordNode> nodes = new ArrayList<>(ListUtils.union(processedApprovalNodes, pendingApprovalNodes));
		List<String> allNodeIds = nodes.stream().map(ApprovalRecordNode::getNodeId).distinct().toList();
		Map<String, ApprovalNodeApprover> approverNodeMap = getApproverNodeMapByIds(allNodeIds);
		Map<String, ApprovalNode> approvalNodeMap = getApprovalNodeMapByIds(allNodeIds);
		nodes.forEach(node -> {
			ApprovalNodeApprover approverNode = approverNodeMap.get(node.getNodeId());
			if (approverNode != null) {
				node.setMultiApproverMode(MultiApproverModeEnum.valueOf(approverNode.getMultiApproverMode()));
				if (CollectionUtils.isNotEmpty(node.getTaskNodes()) && node.getTaskNodes().size() > 1 && StringUtils.isBlank(node.getApprovalStatus())) {
					ApprovalStatus approvalStatusOfMultiNode = getNodeApprovalStatusOfMultiTask(node.getTaskNodes(), node.getMultiApproverMode());
					node.setApprovalStatus(approvalStatusOfMultiNode == null ? null : approvalStatusOfMultiNode.name());
				}
			}
			ApprovalNode approvalNode = approvalNodeMap.get(node.getNodeId());
			node.setNodeName(approvalNode.getName());
			node.setSort(approvalNode.getSort());
		});
		// 节点流程配置顺序
		nodes.sort(Comparator.comparing(ApprovalRecordNode::getSort));
		return nodes;
	}

	/**
	 * 已处理的审批记录节点信息
	 * @param tasks 任务集合
	 * @param records 记录集合
	 * @param addSignTaskMap 加签任务集合
	 * @param attachmentsMap 附件集合
	 * @param simpleUserMap 用户信息集合
	 * @return 审批记录节点集合
	 */
	private List<ApprovalRecordNode> buildProcessedApprovalNodes(List<ApprovalTask> tasks, List<ApprovalRecord> records, Map<String, List<ApprovalAddSignTask>> addSignTaskMap,
														   Map<String, List<Attachment>> attachmentsMap, Map<String, UserSimple> simpleUserMap) {
		List<ApprovalRecordNode> nodes = new ArrayList<>();
		Map<String, ApprovalRecord> autoNodeRecordMap = records.stream().filter(record -> StringUtils.isBlank(record.getTaskId())).collect(Collectors.toMap(ApprovalRecord::getNodeId, r -> r));
		Map<String, ApprovalRecord> taskRecordMap = records.stream().filter(record -> StringUtils.isNotBlank(record.getTaskId())).collect(Collectors.toMap(ApprovalRecord::getTaskId, r -> r));
		List<ApprovalTask> nTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.NL).toList();
		Map<String, Integer> nodeMaxRoundMap = mergeNodeMaxRound(nTasks, records);
		List<String> hisNodes = sortNodeRoundMap(nodeMaxRoundMap, nTasks, records);
		// 处理历史节点
		hisNodes.forEach(hisNode -> {
			Integer maxRound = nodeMaxRoundMap.get(hisNode);
			if (autoNodeRecordMap.containsKey(hisNode) && autoNodeRecordMap.get(hisNode).getNodeRound().equals(maxRound)) {
				// 当前节点的最后一轮执行是自动执行
				ApprovalRecordNode recordNode = ApprovalRecordNode.builder().nodeId(hisNode).nodeRound(maxRound).approvalStatus(autoNodeRecordMap.get(hisNode).getResult()).build();
				nodes.addLast(recordNode);
				return;
			}
			// 获取节点下最后一轮抄送任务
			List<ApprovalTask> ccTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.CC
					&& Strings.CI.equals(task.getNodeId(), hisNode) && task.getNodeRound().equals(maxRound)).toList();
			List<ApprovalCcNode> ccNodes = ccTasks.stream().map(cc -> {
				ApprovalCcNode ccNode = new ApprovalCcNode();
				ccNode.setCcUserId(cc.getApproverId());
				if (simpleUserMap.containsKey(cc.getApproverId())) {
					ccNode.setCcUserName(simpleUserMap.get(cc.getApproverId()).getName());
					ccNode.setCcUserAvatar(simpleUserMap.get(cc.getApproverId()).getAvatar());
				}
				return ccNode;
			}).toList();
			// 获取节点下最后一轮正常待办任务 (排除正常加签操作)
			// 加签场景下, 同一节点可能存在多条rootTask, 只展示最新创建的那个待办
			List<ApprovalTask> nlTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.NL && Strings.CI.equals(task.getNodeId(), hisNode)
					&& task.getNodeRound().equals(maxRound)).sorted(Comparator.comparing(ApprovalTask::getCreateTime)).toList();
			List<ApprovalTask> snTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.SN && Strings.CI.equals(task.getNodeId(), hisNode)
					&& task.getNodeRound().equals(maxRound)).sorted(Comparator.comparing(ApprovalTask::getCreateTime)).toList();
			// 加签任务追加
			if (nlTasks.size() == 1) {
				// 单人执行
				List<ApprovalTask> flatSignTasks = flatSignTask(nlTasks.getFirst(), addSignTaskMap, snTasks.stream().collect(Collectors.toMap(ApprovalTask::getId, t -> t)));
				flatSignTasks.forEach(signTask -> {
					ApprovalTaskNode taskNode = buildTaskNode(signTask, taskRecordMap, attachmentsMap, simpleUserMap);
					ApprovalRecordNode recordNode = ApprovalRecordNode.builder().nodeId(hisNode).nodeRound(maxRound).approvalStatus(taskNode.getApprovalStatus()).taskNodes(List.of(taskNode)).build();
					if (ApprovalTaskType.valueOf(signTask.getType()) == ApprovalTaskType.NL) {
						recordNode.setCcNodes(ccNodes);
					}
					nodes.addLast(recordNode);
				});
			} else {
				List<ApprovalTask> allTask = new ArrayList<>();
				// 多人执行, 追加在同一节点上
				nlTasks.forEach(nlTask -> {
					List<ApprovalTask> flatSignTasks = flatSignTask(nlTask, addSignTaskMap, snTasks.stream().collect(Collectors.toMap(ApprovalTask::getId, t -> t)));
					allTask.addAll(flatSignTasks);
				});
				List<ApprovalTaskNode> taskNodes = allTask.stream().map(nTask -> buildTaskNode(nTask, taskRecordMap, attachmentsMap, simpleUserMap)).toList();
				ApprovalRecordNode recordNode = ApprovalRecordNode.builder().nodeId(hisNode).nodeRound(maxRound).taskNodes(taskNodes).ccNodes(ccNodes).build();
				nodes.addLast(recordNode);
			}
		});
		return nodes;
	}

	/**
	 * 获取后续待审批节点列表
	 * @param instance 审批实例
	 * @param simpleUserMap 用户信息映射
	 * @return 后续待审批节点列表
	 */
	private List<ApprovalRecordNode> buildPendingApprovalNodes(ApprovalInstance instance, Map<String, UserSimple> simpleUserMap, String currentOrgId) {
		if (StringUtils.isBlank(instance.getCurrentNodeId())) {
			// 当前节点为空, 或非审批中的实例
			return new ArrayList<>();
		}
		List<ApprovalNodeApproverResponse> nodes = approvalFlowService.getInstanceCurrentFollowNode(instance, currentOrgId);
		return nodes.stream().map(node -> {
			List<ApprovalTaskNode> taskNodes = node.getApproverList().stream().map(approver -> {
				ApprovalTaskNode taskNode = ApprovalTaskNode.builder().taskId(IDGenerator.nextStr()).approverId(approver).approvalStatus(ApprovalStatus.PENDING.name()).build();
				if (simpleUserMap.containsKey(approver)) {
					taskNode.setApprover(simpleUserMap.get(approver).getName());
					taskNode.setApproverAvatar(simpleUserMap.get(approver).getAvatar());
				}
				return taskNode;
			}).toList();
			return ApprovalRecordNode.builder().nodeId(node.getId()).nodeRound(1).approvalStatus(ApprovalStatus.PENDING.name()).taskNodes(taskNodes).build();
		}).toList();
	}


	/**
	 * 处理多人节点的审批状态
	 * @param taskNodes 任务节点
	 * @param approverMode 多人审批方式
	 * @return 审批状态
	 */
	private ApprovalStatus getNodeApprovalStatusOfMultiTask(List<ApprovalTaskNode> taskNodes, MultiApproverModeEnum approverMode) {
		boolean anyReject = taskNodes.stream().anyMatch(tn -> ApprovalStatus.valueOf(tn.getApprovalStatus()) == ApprovalStatus.UNAPPROVED);
		boolean anyAutoReject = taskNodes.stream().anyMatch(tn -> ApprovalStatus.valueOf(tn.getApprovalStatus()) == ApprovalStatus.AUTO_UNAPPROVED);
		boolean anyApproved = taskNodes.stream().anyMatch(tn -> ApprovalStatus.valueOf(tn.getApprovalStatus()) == ApprovalStatus.APPROVED);
		boolean anyAutoApproved = taskNodes.stream().anyMatch(tn -> ApprovalStatus.valueOf(tn.getApprovalStatus()) == ApprovalStatus.AUTO_APPROVED);
		if (approverMode == MultiApproverModeEnum.ALL || approverMode == MultiApproverModeEnum.SEQUENTIAL) {
			// 会签, 依次审批  (状态优先级: 驳回 > 自动驳回 -> 已通过 -> 自动通过)
			if (anyReject) {
				return ApprovalStatus.UNAPPROVED;
			}
			if (anyAutoReject) {
				return ApprovalStatus.AUTO_UNAPPROVED;
			}
			if (anyApproved) {
				return ApprovalStatus.APPROVED;
			}
			if (anyAutoApproved) {
				return ApprovalStatus.AUTO_APPROVED;
			}
		} else if (approverMode == MultiApproverModeEnum.ANY){
			// 或签 (状态优先级: 已通过 > 自动通过 -> 自动驳回 -> 驳回)
			if (anyApproved) {
				return ApprovalStatus.APPROVED;
			}
			if (anyAutoApproved) {
				return ApprovalStatus.AUTO_APPROVED;
			}
			if (anyAutoReject) {
				return ApprovalStatus.AUTO_UNAPPROVED;
			}
			if (anyReject) {
				return ApprovalStatus.UNAPPROVED;
			}
		}
		return null;
	}

	/**
	 * 获取所有审批节点集合
	 * @param nodeIds 节点ID集合
	 * @return 审批节点集合
	 */
	private Map<String, ApprovalNodeApprover> getApproverNodeMapByIds(List<String> nodeIds) {
		if (CollectionUtils.isEmpty(nodeIds)) {
			return Map.of();
		}
		List<ApprovalNodeApprover> approvalNodeApprovers = approvalNodeApproverMapper.selectByIds(nodeIds);
		return approvalNodeApprovers.stream().collect(Collectors.toMap(ApprovalNodeApprover::getId, n -> n));
	}

	/**
	 * 获取所有审批节点集合
	 * @param nodeIds 节点ID集合
	 * @return 审批节点集合
	 */
	private Map<String, ApprovalNode> getApprovalNodeMapByIds(List<String> nodeIds) {
		if (CollectionUtils.isEmpty(nodeIds)) {
			return Map.of();
		}
		List<ApprovalNode> approvalNodes = approvalNodeMapper.selectByIds(nodeIds);
		return approvalNodes.stream().collect(Collectors.toMap(ApprovalNode::getId, n -> n));
	}

	private List<ApprovalTask> flatSignTask(ApprovalTask currentTask, Map<String, List<ApprovalAddSignTask>> addSignTaskMap, Map<String, ApprovalTask> signTaskMap) {
		if (!signTaskMap.containsKey(currentTask.getId())) {
			// 不存在加签链
			return List.of(currentTask);
		}
		List<ApprovalAddSignTask> signTasks = addSignTaskMap.get(currentTask.getId());
		Optional<ApprovalAddSignTask> rootNext = signTasks.stream().filter(signTask -> Strings.CS.equals(signTask.getSignTaskId(), currentTask.getId())).findFirst();
		if (rootNext.isEmpty()) {
			// 不存在加签链
			return List.of(currentTask);
		}
		signTasks.sort(Comparator.comparing(ApprovalAddSignTask::getSort));
		List<ApprovalTask> signChain = new ArrayList<>();
		if (ApprovalAddSignType.valueOf(rootNext.get().getType()) == ApprovalAddSignType.BEFORE) {
			// 下一个节点在基础节点之前
			signChain.addAll(signTasks.stream().map(signTask -> signTaskMap.get(signTask.getTaskId())).toList());
			signChain.addLast(currentTask);
		} else {
			// 下一个节点在基础节点之后
			signChain.addFirst(currentTask);
			signChain.addAll(signTasks.stream().map(signTask -> signTaskMap.get(signTask.getTaskId())).toList());
		}
		return signChain;
	}

	/**
	 * 获取最终的节点顺序 (同一节点可能执行多轮)
	 * @param tasks 节点待办
	 * @param records 节点执行记录
	 * @return 节点ID -> 最大轮次
	 */
	private Map<String, Integer> mergeNodeMaxRound(List<ApprovalTask> tasks, List<ApprovalRecord> records) {
		// 待办中提取节点和最大轮次
		Map<String, Integer> taskNodeMaxRoundMap = tasks.stream().collect(Collectors.toMap(ApprovalTask::getNodeId, ApprovalTask::getNodeRound, Math::max));
		// 记录中提取节点和最大轮次
		Map<String, Integer> recordNodeMaxRoundMap = records.stream().collect(Collectors.toMap(ApprovalRecord::getNodeId, ApprovalRecord::getNodeRound, Math::max));
		// 合并节点集合, 按照最大轮次保留
		Map<String, Integer> mergedMap = new HashMap<>(recordNodeMaxRoundMap);
		taskNodeMaxRoundMap.forEach((nodeId, nodeRound) -> mergedMap.merge(nodeId, nodeRound, Math::max));
		return mergedMap;
	}

	/**
	 * 轮次节点的最终执行排序
	 * 按照该节点的最大轮次中的最小创建时间来排序，早的在前面
	 * @param nodeRoundMap 节点ID -> 最大轮次
	 * @param tasks 所有待办
	 * @param records 所有记录
	 * @return 排序后的节点ID列表
	 */
	private List<String> sortNodeRoundMap(Map<String, Integer> nodeRoundMap, List<ApprovalTask> tasks, List<ApprovalRecord> records) {
		// 找出每个节点最大轮次中的最小创建时间
		Map<String, Long> nodeMinCreateTimeMap = new HashMap<>(nodeRoundMap.size());

		// 从待办中找：筛选出每个节点最大轮次的待办，取最小创建时间
		tasks.forEach(task -> {
			Integer maxRound = nodeRoundMap.get(task.getNodeId());
			if (task.getNodeRound() != null && task.getNodeRound().equals(maxRound)) {
				nodeMinCreateTimeMap.merge(task.getNodeId(), task.getCreateTime(), Math::min);
			}
		});

		// 从记录中找：筛选出每个节点最大轮次的记录，取最小创建时间
		records.forEach(record -> {
			Integer maxRound = nodeRoundMap.get(record.getNodeId());
			if (record.getNodeRound() != null && record.getNodeRound().equals(maxRound)) {
				nodeMinCreateTimeMap.merge(record.getNodeId(), record.getCreateTime(), Math::min);
			}
		});

		// 按最小创建时间升序排序
		return nodeMinCreateTimeMap.entrySet().stream()
			.sorted(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}

	private ApprovalTaskNode buildTaskNode(ApprovalTask task, Map<String, ApprovalRecord> taskRecordMap, Map<String, List<Attachment>> attachmentsMap, Map<String, UserSimple> simpleUserMap) {
		ApprovalRecord record = taskRecordMap.get(task.getId());
		ApprovalTaskNode taskNode = ApprovalTaskNode.builder().taskId(task.getId()).sign(ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.SN)
				.approverId(task.getApproverId()).approvalStatus(task.getStatus()).build();
		if (simpleUserMap.containsKey(task.getApproverId())) {
			taskNode.setApprover(simpleUserMap.get(task.getApproverId()).getName());
			taskNode.setApproverAvatar(simpleUserMap.get(task.getApproverId()).getAvatar());
		}
		if (record != null) {
			taskNode.setComment(record.getComment());
			taskNode.setAttachments(attachmentsMap.get(record.getId()));
			taskNode.setApprovalTime(record.getCreateTime());
		}
		return taskNode;
	}

	/**
	 * 刷新最终实例状态
	 *
	 * @param instance     审批实例
	 * @param currentUserId  当前用户ID
	 */
	public void rejectApprovalInstance(ApprovalInstance instance, String currentUserId) {
		instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
		instance.setApprovalTime(System.currentTimeMillis());
		instance.setUpdateUser(currentUserId);
		instance.setUpdateTime(System.currentTimeMillis());
		approvalInstanceMapper.updateById(instance);
	}

	/**
	 * 清除审批中的实例数据 (审批流删除时调用)
	 * @param flowId 审批流ID
	 */
	public void clearApprovingInstanceOfFlow(String flowId) {
		ApprovalFlowVersion versionCriteria = new ApprovalFlowVersion();
		versionCriteria.setFlowId(flowId);
		List<ApprovalFlowVersion> versions = flowVersionMapper.select(versionCriteria);
		List<String> flowVersionIds = versions.stream().map(ApprovalFlowVersion::getId).toList();
		// 筛选出审批中的实例
		LambdaQueryWrapper<ApprovalInstance> instanceLambdaQueryWrapper = new LambdaQueryWrapper<ApprovalInstance>()
				.in(ApprovalInstance::getFlowVersionId, flowVersionIds)
				.eq(ApprovalInstance::getApprovalStatus, ApprovalStatus.APPROVING.name());
		List<ApprovalInstance> instances = approvalInstanceMapper.selectListByLambda(instanceLambdaQueryWrapper);
		instances.forEach(instance -> {
			ApprovalResourceService resourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
			if (resourceService != null) {
				resourceService.updateResourceApprovalStatus(FormKey.ofKey(instance.getType()), instance.getResourceId(), ApprovalStatus.NONE.name());
			}
		});
		approvalInstanceMapper.deleteByLambda(instanceLambdaQueryWrapper);
		// 删除审批实例相关数据: 待办任务、加签任务、退回记录、执行记录、实例附件等
		List<String> instanceIds = instances.stream().map(ApprovalInstance::getId).toList();
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>().in(ApprovalTask::getInstanceId, instanceIds));
		List<String> taskIds = approvalTasks.stream().map(ApprovalTask::getId).toList();
		if (CollectionUtils.isNotEmpty(taskIds)) {
			approvalTaskMapper.deleteByIds(taskIds);
			approvalAddSignTaskMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalAddSignTask>().in(ApprovalAddSignTask::getTaskId, taskIds));
			approvalReturnBackRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalReturnBackRecord>().in(ApprovalReturnBackRecord::getTaskId, taskIds));
		}
		if (CollectionUtils.isNotEmpty(taskIds)) {
			approvalRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalRecord>().in(ApprovalRecord::getInstanceId, instanceIds));
			List<ApprovalInstanceAttachment> instanceAttachments = approvalInstanceAttachmentMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalInstanceAttachment>().in(ApprovalInstanceAttachment::getInstanceId, instanceIds));
			approvalInstanceAttachmentMapper.deleteByIds(instanceAttachments.stream().map(ApprovalInstanceAttachment::getId).toList());
			List<String> attachmentIds = instanceAttachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).toList();
			attachmentIds.forEach(id -> attachmentService.delete(id));
			attachmentMapper.deleteByIds(attachmentIds);
		}
	}
}
